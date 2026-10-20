package com.fitnessclub.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.ClubZone;
import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.DocumentRequestDto;
import com.fitnessclub.api.dto.DocumentResponseDto;
import com.fitnessclub.api.dto.TrainerCreateRequest;
import com.fitnessclub.api.dto.TrainerResponseDto;
import com.fitnessclub.api.dto.TrainerUpdateRequest;
import com.fitnessclub.domain.DocumentEntity;
import com.fitnessclub.domain.DocumentType;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.Trainer;
import com.fitnessclub.domain.TrainerZone;
import com.fitnessclub.domain.Zone;
import com.fitnessclub.repository.AdminRepository;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.DocumentRepository;
import com.fitnessclub.repository.PersonalDataRepository;
import com.fitnessclub.repository.ScheduleSlotRepository;
import com.fitnessclub.repository.TrainerRepository;
import com.fitnessclub.repository.TrainerZoneRepository;
import com.fitnessclub.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminTrainerService {

	private static final int MIN_AGE_TRAINER = 18;

	private static final int MAX_AGE_TRAINER = 70;

	private static final Pattern TRAINER_BIO_ALLOWED = Pattern.compile("^[\\p{L}\\p{N}\\s]+$");

	private final TrainerRepository trainerRepository;
	private final ClientRepository clientRepository;
	private final AdminRepository adminRepository;
	private final PersonalDataRepository personalDataRepository;
	private final TrainerZoneRepository trainerZoneRepository;
	private final ZoneRepository zoneRepository;
	private final DocumentRepository documentRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final PersonalDataRulesService personalRules;
	private final DocumentRulesService documentRules;
	private final PasswordEncoder passwordEncoder;

	@Transactional(readOnly = true)
	public List<TrainerResponseDto> listAll() {
		return trainerRepository.findAllWithPersonalData().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> availableClientsForTrainerRole() {
		List<Map<String, Object>> out = new java.util.ArrayList<>();
		for (Client c : clientRepository.findNotTrainerWithPersonalOrderByName()) {
			PersonalData p = c.getPersonalData();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("client_id", c.getClientId());
			row.put("personal_data_id", p.getPersonalDataId());
			row.put("full_name", (p.getLastName() + " " + p.getFirstName()
					+ (p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : " " + p.getMiddleName())).trim());
			out.add(row);
		}
		return out;
	}

	@Transactional
	public TrainerResponseDto createFromClient(Integer clientId) {
		Client client = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		Integer pdId = client.getPersonalData().getPersonalDataId();
		if (trainerRepository.existsByPersonalData_PersonalDataId(pdId)) {
			throw ApiValidationException.single("client_id", "Для выбранного клиента уже создан тренер.");
		}
		Trainer trainer = new Trainer();
		trainer.setPersonalData(client.getPersonalData());
		trainer.setSpecialization("Не указано");
		trainer.setExperienceYears(0);
		trainer.setRating(null);
		trainer.setBiography("Не указано");
		trainer.setIsActive(Boolean.FALSE);
		trainerRepository.save(trainer);
		return toDto(trainerRepository.findByIdFetchPersonal(trainer.getTrainerId()).orElse(trainer));
	}

	@Transactional
	public TrainerResponseDto create(TrainerCreateRequest req) {
		Map<String, String> errors = new LinkedHashMap<>();
		validateTrainerProfessional(req.getSpecialization(), req.getExperienceYears(), req.getBiography(), req.getRating(), errors);
		personalRules.validatePersonNames(req.getLastName(), req.getFirstName(), req.getMiddleName(), errors);
		personalRules.validateBirthDate(req.getBirthDate(), MIN_AGE_TRAINER, MAX_AGE_TRAINER, errors, "birth_date");
		personalRules.validateGender(req.getGender(), errors);
		personalRules.validateEmailFormat(req.getEmail(), errors);
		personalRules.validatePhoneRaw(req.getPhone(), errors);
		TextNormalizationUtils.validateAddress(req.getRegistrationAddress(), "registration_address", true, errors);
		TextNormalizationUtils.validateAddress(req.getActualAddress(), "actual_address", false, errors);
		if (req.getPassword() == null || req.getPasswordConfirm() == null
				|| !req.getPassword().equals(req.getPasswordConfirm())) {
			errors.put("password_confirm", "Пароль и подтверждение должны совпадать.");
		} else {
			TextNormalizationUtils.validatePasswordStrength(req.getPassword(), errors, "password");
		}
		throwIfErrors(errors);

		String phoneNorm = TextNormalizationUtils.normalizeRussianPhone(req.getPhone());
		String phoneStore = "+" + phoneNorm;
		String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
		if (personalDataRepository.existsByEmailIgnoreCase(email)) {
			throw ApiValidationException.single("email", "Email уже занят.");
		}
		if (personalDataRepository.existsByPhone(phoneStore)) {
			throw ApiValidationException.single("phone", "Телефон уже занят.");
		}

		PersonalData pd = new PersonalData();
		applyPersonal(pd, req, phoneStore, email, passwordEncoder.encode(req.getPassword()));

		personalDataRepository.save(pd);

		Trainer t = new Trainer();
		t.setPersonalData(pd);
		t.setSpecialization(req.getSpecialization().trim());
		t.setExperienceYears(req.getExperienceYears());
		t.setRating(req.getRating());
		t.setBiography(req.getBiography() == null ? null : req.getBiography().trim());
		t.setIsActive(Boolean.FALSE);
		trainerRepository.save(t);

		replaceZones(t.getTrainerId(), req.getZoneIds());
		return toDto(trainerRepository.findByIdFetchPersonal(t.getTrainerId()).orElse(t));
	}

	@Transactional
	public TrainerResponseDto update(Integer trainerId, TrainerUpdateRequest req) {
		Trainer t = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		PersonalData pd = t.getPersonalData();

		Map<String, String> errors = new LinkedHashMap<>();
		validateTrainerProfessional(req.getSpecialization(), req.getExperienceYears(), req.getBiography(), req.getRating(), errors);
		personalRules.validatePersonNames(req.getLastName(), req.getFirstName(), req.getMiddleName(), errors);
		personalRules.validateBirthDate(req.getBirthDate(), MIN_AGE_TRAINER, MAX_AGE_TRAINER, errors, "birth_date");
		personalRules.validateGender(req.getGender(), errors);
		personalRules.validateEmailFormat(req.getEmail(), errors);
		if (req.getPhone() == null || req.getPhone().isBlank()) {
			errors.put("phone", "Телефон обязателен.");
		} else {
			String normalized = TextNormalizationUtils.normalizeRussianPhone(req.getPhone());
			if (!TextNormalizationUtils.isValidNormalizedPhone(normalized)) {
				errors.put("phone", "Телефон: российский номер, после нормализации формат +7 и 10 цифр.");
			}
		}
		TextNormalizationUtils.validateAddress(req.getRegistrationAddress(), "registration_address", true, errors);
		TextNormalizationUtils.validateAddress(req.getActualAddress(), "actual_address", false, errors);

		boolean changePw = req.getNewPassword() != null && !req.getNewPassword().isBlank();
		if (changePw) {
			if (!req.getNewPassword().equals(req.getNewPasswordConfirm())) {
				errors.put("new_password_confirm", "Пароли не совпадают.");
			} else {
				TextNormalizationUtils.validatePasswordStrength(req.getNewPassword(), errors, "new_password");
			}
		}
		throwIfErrors(errors);

		boolean wasActive = Boolean.TRUE.equals(t.getIsActive());
		Boolean requestedActive = req.getIsActive();
		if (Boolean.TRUE.equals(requestedActive) && !wasActive) {
			List<DocumentEntity> docs = documentRepository
					.findByPersonalDataPersonalDataIdOrderByDocumentId(pd.getPersonalDataId());
			List<String> miss = TrainerActiveDocumentRequirements.missingVerifiedRequirements(docs);
			if (!miss.isEmpty()) {
				throw ApiValidationException.single("is_active",
						"Активация невозможна без полного набора проверенных документов. Не хватает: "
								+ String.join("; ", miss) + ".");
			}
		}
		if (Boolean.FALSE.equals(requestedActive) && wasActive
				&& scheduleSlotRepository.existsByTrainer_TrainerIdAndStartDatetimeGreaterThanEqual(trainerId,
						LocalDateTime.now(ClubZone.ZONE))) {
			throw ApiValidationException.single("is_active",
					"Нельзя установить статус «Не активен»: у тренера есть предстоящие занятия в расписании.");
		}

		String phoneNorm = TextNormalizationUtils.normalizeRussianPhone(req.getPhone());
		String phoneStore = "+" + phoneNorm;
		String existingPhoneNorm = TextNormalizationUtils.normalizeRussianPhone(pd.getPhone());
		boolean phoneChanged = !Objects.equals(existingPhoneNorm, phoneNorm);
		String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
		if (personalDataRepository.existsByEmailIgnoreCaseAndPersonalDataIdNot(email, pd.getPersonalDataId())) {
			throw ApiValidationException.single("email", "Email уже занят.");
		}
		if (phoneChanged && personalDataRepository.existsByPhoneAndPersonalDataIdNot(phoneStore, pd.getPersonalDataId())) {
			throw ApiValidationException.single("phone", "Телефон уже занят.");
		}

		applyPersonalUpdate(pd, req, phoneStore, email, changePw ? passwordEncoder.encode(req.getNewPassword()) : pd.getPasswordHash());

		t.setSpecialization(req.getSpecialization().trim());
		t.setExperienceYears(req.getExperienceYears());
		t.setRating(req.getRating());
		t.setBiography(req.getBiography() == null ? null : req.getBiography().trim());
		if (requestedActive != null) {
			t.setIsActive(Boolean.TRUE.equals(requestedActive));
		}

		replaceZones(trainerId, req.getZoneIds());
		return toDto(t);
	}

	@Transactional
	public void delete(Integer trainerId) {
		Trainer t = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		if (scheduleSlotRepository.existsByTrainer_TrainerIdAndStartDatetimeGreaterThanEqual(trainerId,
				LocalDateTime.now(ClubZone.ZONE))) {
			throw ApiValidationException.single("trainer_id",
					"Нельзя удалить тренера: у него есть предстоящие занятия в расписании. Сначала отмените или перенесите их.");
		}
		Integer pdId = t.getPersonalData().getPersonalDataId();
		trainerZoneRepository.deleteByTrainerId(trainerId);
		trainerRepository.delete(t);
		boolean hasClientRole = clientRepository.existsByPersonalData_PersonalDataId(pdId);
		boolean hasAdminRole = adminRepository.existsByPersonalDataId(pdId);
		if (!hasClientRole && !hasAdminRole) {
			personalDataRepository.deleteById(pdId);
		}
	}

	@Transactional(readOnly = true)
	public List<DocumentResponseDto> listDocuments(Integer trainerId) {
		Trainer t = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		return documentRepository.findByPersonalDataPersonalDataIdOrderByDocumentId(t.getPersonalData().getPersonalDataId())
				.stream().map(this::toDocDto).collect(Collectors.toList());
	}

	@Transactional
	public DocumentResponseDto createDocument(Integer trainerId, DocumentRequestDto dto) {
		Trainer t = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		PersonalData pd = t.getPersonalData();
		String number = normalizeNumberForType(dto.getDocumentType(), dto.getNumber());
		documentRules.validate(dto.getDocumentType(), dto.getSeries(), number, dto.getIssuedBy(),
				dto.getIssueDate(), dto.getExpiryDate());
		if (documentRepository.existsByPersonalDataPersonalDataIdAndDocumentTypeAndNumber(
				pd.getPersonalDataId(), dto.getDocumentType(), number)) {
			throw ApiValidationException.single("number", "Документ с таким типом и номером уже существует.");
		}
		DocumentEntity e = new DocumentEntity();
		e.setPersonalData(pd);
		applyDocument(e, dto, number);
		documentRepository.save(e);
		return toDocDto(e);
	}

	@Transactional
	public DocumentResponseDto updateDocument(Integer trainerId, Integer documentId, DocumentRequestDto dto) {
		Trainer t = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		Integer pdId = t.getPersonalData().getPersonalDataId();
		DocumentEntity e = documentRepository.findByDocumentIdAndPersonalDataPersonalDataId(documentId, pdId)
				.orElseThrow(() -> ApiValidationException.single("document_id", "Документ не найден."));
		String number = normalizeNumberForType(dto.getDocumentType(), dto.getNumber());
		documentRules.validate(dto.getDocumentType(), dto.getSeries(), number, dto.getIssuedBy(),
				dto.getIssueDate(), dto.getExpiryDate());
		if (documentRepository.existsByPersonalDataPersonalDataIdAndDocumentTypeAndNumberAndDocumentIdNot(
				pdId, dto.getDocumentType(), number, documentId)) {
			throw ApiValidationException.single("number", "Документ с таким типом и номером уже существует.");
		}
		applyDocument(e, dto, number);
		documentRepository.save(e);
		return toDocDto(e);
	}

	@Transactional
	public void deleteDocument(Integer trainerId, Integer documentId) {
		Trainer t = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		Integer pdId = t.getPersonalData().getPersonalDataId();
		DocumentEntity e = documentRepository.findByDocumentIdAndPersonalDataPersonalDataId(documentId, pdId)
				.orElseThrow(() -> ApiValidationException.single("document_id", "Документ не найден."));
		documentRepository.delete(e);
	}

	private void validateTrainerProfessional(String spec, Integer expYears, String bio, BigDecimal rating, Map<String, String> errors) {
		if (spec == null || spec.isBlank()) {
			errors.put("specialization", "Специализация обязательна.");
		}
		if (expYears == null || expYears < 0 || expYears > 50) {
			errors.put("experience_years", "Опыт: от 0 до 50 лет.");
		}
		if (rating != null) {
			if (rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(BigDecimal.TEN) > 0) {
				errors.put("rating", "Рейтинг: от 1 до 10.");
			} else if (rating.scale() > 2) {
				errors.put("rating", "Рейтинг: не более двух знаков после запятой.");
			}
		}
		if (bio == null || bio.isBlank()) {
			errors.put("biography", "Биография обязательна.");
		} else {
			String b = bio.trim();
			if (b.length() > 100) {
				errors.put("biography", "Биография: не более 100 символов.");
			} else if (!TRAINER_BIO_ALLOWED.matcher(b).matches()) {
				errors.put("biography", "Биография: только буквы, цифры и пробелы.");
			}
		}
	}

	private void applyPersonal(PersonalData pd, TrainerCreateRequest req, String phone, String email, String pwHash) {
		pd.setLastName(TextNormalizationUtils.capitalizeCyrillicNamePart(req.getLastName().trim()));
		pd.setFirstName(TextNormalizationUtils.capitalizeCyrillicNamePart(req.getFirstName().trim()));
		pd.setMiddleName(req.getMiddleName() == null || req.getMiddleName().isBlank() ? null
				: TextNormalizationUtils.capitalizeCyrillicNamePart(req.getMiddleName().trim()));
		pd.setBirthDate(req.getBirthDate());
		pd.setPhone(phone);
		pd.setEmail(email);
		pd.setGender(req.getGender());
		pd.setRegistrationAddress(req.getRegistrationAddress().trim());
		pd.setActualAddress(req.getActualAddress() == null || req.getActualAddress().isBlank() ? null : req.getActualAddress().trim());
		pd.setPasswordHash(pwHash);
	}

	private void applyPersonalUpdate(PersonalData pd, TrainerUpdateRequest req, String phone, String email, String pwHash) {
		pd.setLastName(TextNormalizationUtils.capitalizeCyrillicNamePart(req.getLastName().trim()));
		pd.setFirstName(TextNormalizationUtils.capitalizeCyrillicNamePart(req.getFirstName().trim()));
		pd.setMiddleName(req.getMiddleName() == null || req.getMiddleName().isBlank() ? null
				: TextNormalizationUtils.capitalizeCyrillicNamePart(req.getMiddleName().trim()));
		pd.setBirthDate(req.getBirthDate());
		pd.setPhone(phone);
		pd.setEmail(email);
		pd.setGender(req.getGender());
		pd.setRegistrationAddress(req.getRegistrationAddress().trim());
		pd.setActualAddress(req.getActualAddress() == null || req.getActualAddress().isBlank() ? null : req.getActualAddress().trim());
		pd.setPasswordHash(pwHash);
	}

	private void replaceZones(Integer trainerId, List<Integer> zoneIds) {
		trainerZoneRepository.deleteByTrainerId(trainerId);
		if (zoneIds == null) {
			return;
		}
		LinkedHashSet<Integer> uniqueZoneIds = zoneIds.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		for (Integer zid : uniqueZoneIds) {
			if (!zoneRepository.existsById(zid)) {
				throw ApiValidationException.single("zone_ids", "Недопустимый идентификатор зоны: " + zid);
			}
			TrainerZone tz = new TrainerZone();
			tz.setTrainerId(trainerId);
			tz.setZoneId(zid);
			trainerZoneRepository.save(tz);
		}
	}

	private void applyDocument(DocumentEntity e, DocumentRequestDto dto, String number) {
		e.setDocumentType(dto.getDocumentType());
		e.setSeries(blankNull(dto.getSeries()));
		e.setNumber(number);
		e.setIssuedBy(blankNull(dto.getIssuedBy()));
		e.setIssueDate(dto.getIssueDate());
		e.setExpiryDate(dto.getExpiryDate());
		e.setNotes(blankNull(dto.getNotes()));
		e.setVerified(Boolean.TRUE.equals(dto.getVerified()));
	}

	private String normalizeNumberForType(DocumentType type, String raw) {
		if (raw == null) {
			return "";
		}
		return switch (type) {
			case INN -> documentRules.cleanInn(raw);
			case SNILS -> documentRules.cleanSnils(raw);
			default -> raw.trim();
		};
	}

	private String blankNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	private TrainerResponseDto toDto(Trainer t) {
		PersonalData p = t.getPersonalData();
		List<Integer> z = trainerZoneRepository.findByTrainerId(t.getTrainerId()).stream()
				.map(TrainerZone::getZoneId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		Map<Integer, String> zoneTitles = zoneRepository.findAllById(z).stream()
				.collect(Collectors.toMap(Zone::getZoneId, Zone::getZoneName, (a, b) -> a));
		List<String> zoneNames = z.stream()
				.map(id -> zoneTitles.getOrDefault(id, "—"))
				.collect(Collectors.toList());
		return TrainerResponseDto.builder()
				.trainerId(t.getTrainerId())
				.personalDataId(p.getPersonalDataId())
				.lastName(p.getLastName())
				.firstName(p.getFirstName())
				.middleName(p.getMiddleName())
				.birthDate(p.getBirthDate())
				.phone(p.getPhone())
				.email(p.getEmail())
				.gender(p.getGender())
				.specialization(t.getSpecialization())
				.experienceYears(t.getExperienceYears())
				.rating(t.getRating())
				.isActive(!Boolean.FALSE.equals(t.getIsActive()))
				.biography(t.getBiography())
				.zoneIds(z)
				.zoneNames(zoneNames)
				.registrationAddress(p.getRegistrationAddress())
				.actualAddress(p.getActualAddress())
				.build();
	}

	private DocumentResponseDto toDocDto(DocumentEntity d) {
		return DocumentResponseDto.builder()
				.documentId(d.getDocumentId())
				.personalDataId(d.getPersonalData().getPersonalDataId())
				.documentType(d.getDocumentType())
				.series(d.getSeries())
				.number(d.getNumber())
				.issuedBy(d.getIssuedBy())
				.issueDate(d.getIssueDate())
				.expiryDate(d.getExpiryDate())
				.notes(d.getNotes())
				.verified(Boolean.TRUE.equals(d.getVerified()))
				.build();
	}

	private void throwIfErrors(Map<String, String> errors) {
		if (!errors.isEmpty()) {
			throw new ApiValidationException(errors);
		}
	}
}
