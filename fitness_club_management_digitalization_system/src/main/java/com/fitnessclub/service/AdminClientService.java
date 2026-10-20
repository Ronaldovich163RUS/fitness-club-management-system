package com.fitnessclub.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.ClientCreateRequest;
import com.fitnessclub.api.dto.ClientResponseDto;
import com.fitnessclub.api.dto.ClientUpdateRequest;
import com.fitnessclub.api.dto.DocumentRequestDto;
import com.fitnessclub.api.dto.DocumentResponseDto;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.DocumentEntity;
import com.fitnessclub.domain.DocumentType;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.MembershipPurchase;
import com.fitnessclub.domain.MembershipServiceLink;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.Trainer;
import com.fitnessclub.repository.AdminRepository;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.DocumentRepository;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.MembershipPurchaseRepository;
import com.fitnessclub.repository.MembershipServiceLinkRepository;
import com.fitnessclub.repository.MembershipUsageRecordRepository;
import com.fitnessclub.repository.PersonalDataRepository;
import com.fitnessclub.repository.TrainerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminClientService {

	private static final int MIN_AGE_CLIENT = 14;

	private final ClientRepository clientRepository;
	private final PersonalDataRepository personalDataRepository;
	private final DocumentRepository documentRepository;
	private final PersonalDataRulesService personalRules;
	private final DocumentRulesService documentRules;
	private final PasswordEncoder passwordEncoder;
	private final TrainerRepository trainerRepository;
	private final AdminRepository adminRepository;
	private final MembershipPurchaseRepository membershipPurchaseRepository;
	private final MembershipServiceLinkRepository membershipServiceLinkRepository;
	private final MembershipUsageRecordRepository membershipUsageRecordRepository;
	private final FitnessServiceRepository fitnessServiceRepository;

	@Transactional(readOnly = true)
	public List<ClientResponseDto> listAll() {
		return clientRepository.findAllWithPersonalData().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> availableTrainersForClientRole() {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Trainer t : trainerRepository.findNotClientWithPersonalOrderByName()) {
			PersonalData p = t.getPersonalData();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("trainer_id", t.getTrainerId());
			row.put("personal_data_id", p.getPersonalDataId());
			row.put("full_name", (p.getLastName() + " " + p.getFirstName()
					+ (p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : " " + p.getMiddleName())).trim());
			row.put("specialization", t.getSpecialization());
			out.add(row);
		}
		return out;
	}

	@Transactional
	public ClientResponseDto createFromTrainer(Integer trainerId) {
		Trainer trainer = trainerRepository.findByIdFetchPersonal(trainerId)
				.orElseThrow(() -> ApiValidationException.single("trainer_id", "Тренер не найден."));
		Integer pdId = trainer.getPersonalData().getPersonalDataId();
		if (clientRepository.existsByPersonalData_PersonalDataId(pdId)) {
			throw ApiValidationException.single("trainer_id", "Для выбранного тренера уже создан клиент.");
		}
		Client client = new Client();
		client.setPersonalData(trainer.getPersonalData());
		client.setIsActive(Boolean.TRUE);
		clientRepository.save(client);
		return toDto(client);
	}

	@Transactional
	public ClientResponseDto create(ClientCreateRequest req) {
		Map<String, String> errors = new LinkedHashMap<>();
		personalRules.validatePersonNames(req.getLastName(), req.getFirstName(), req.getMiddleName(), errors);
		personalRules.validateBirthDate(req.getBirthDate(), MIN_AGE_CLIENT, errors, "birth_date");
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

		String phoneNorm = personalRules.requireNormalizedPhone(req.getPhone());
		String phoneStore = "+" + phoneNorm;
		String email = req.getEmail().trim().toLowerCase(Locale.ROOT);

		if (personalDataRepository.existsByEmailIgnoreCase(email)) {
			throw ApiValidationException.single("email", "Email уже занят.");
		}
		if (personalDataRepository.existsByPhone(phoneStore)) {
			throw ApiValidationException.single("phone", "Телефон уже занят.");
		}

		PersonalData pd = new PersonalData();
		applyPersonalFromRequest(pd,
				TextNormalizationUtils.capitalizeCyrillicNamePart(req.getLastName().trim()),
				TextNormalizationUtils.capitalizeCyrillicNamePart(req.getFirstName().trim()),
				req.getMiddleName(),
				req.getBirthDate(), phoneStore, email, req.getGender(),
				req.getRegistrationAddress().trim(),
				req.getActualAddress() == null || req.getActualAddress().isBlank() ? null : req.getActualAddress().trim(),
				passwordEncoder.encode(req.getPassword()));

		personalDataRepository.save(pd);

		Client client = new Client();
		client.setPersonalData(pd);
		client.setIsActive(Boolean.TRUE);
		clientRepository.save(client);
		return toDto(client);
	}

	@Transactional
	public ClientResponseDto update(Integer clientId, ClientUpdateRequest req) {
		Client client = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		PersonalData pd = client.getPersonalData();

		Map<String, String> errors = new LinkedHashMap<>();
		personalRules.validatePersonNames(req.getLastName(), req.getFirstName(), req.getMiddleName(), errors);
		personalRules.validateBirthDate(req.getBirthDate(), MIN_AGE_CLIENT, errors, "birth_date");
		personalRules.validateGender(req.getGender(), errors);
		personalRules.validateEmailFormat(req.getEmail(), errors);
		personalRules.validatePhoneRaw(req.getPhone(), errors, pd.getPersonalDataId());
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

		applyPersonalFromRequest(pd,
				TextNormalizationUtils.capitalizeCyrillicNamePart(req.getLastName().trim()),
				TextNormalizationUtils.capitalizeCyrillicNamePart(req.getFirstName().trim()),
				req.getMiddleName(),
				req.getBirthDate(), phoneStore, email, req.getGender(),
				req.getRegistrationAddress().trim(),
				req.getActualAddress() == null || req.getActualAddress().isBlank() ? null : req.getActualAddress().trim(),
				changePw ? passwordEncoder.encode(req.getNewPassword()) : pd.getPasswordHash());
		client.setIsActive(Boolean.TRUE.equals(req.getIsActive()));

		return toDto(client);
	}

	@Transactional
	public void delete(Integer clientId) {
		Client client = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		Integer pdId = client.getPersonalData().getPersonalDataId();
		clientRepository.delete(client);
		boolean hasTrainerRole = trainerRepository.existsByPersonalData_PersonalDataId(pdId);
		boolean hasAdminRole = adminRepository.existsByPersonalDataId(pdId);
		if (!hasTrainerRole && !hasAdminRole) {
			personalDataRepository.deleteById(pdId);
		}
	}

	@Transactional(readOnly = true)
	public List<DocumentResponseDto> listDocuments(Integer clientId) {
		Client c = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		return documentRepository.findByPersonalDataPersonalDataIdOrderByDocumentId(c.getPersonalData().getPersonalDataId())
				.stream().map(this::toDocDto).collect(Collectors.toList());
	}

	@Transactional
	public DocumentResponseDto createDocument(Integer clientId, DocumentRequestDto dto) {
		Client c = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		PersonalData pd = c.getPersonalData();
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
	public DocumentResponseDto updateDocument(Integer clientId, Integer documentId, DocumentRequestDto dto) {
		Client c = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		Integer pdId = c.getPersonalData().getPersonalDataId();
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
		return toDocDto(e);
	}

	@Transactional
	public void deleteDocument(Integer clientId, Integer documentId) {
		Client c = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		Integer pdId = c.getPersonalData().getPersonalDataId();
		DocumentEntity e = documentRepository.findByDocumentIdAndPersonalDataPersonalDataId(documentId, pdId)
				.orElseThrow(() -> ApiValidationException.single("document_id", "Документ не найден."));
		documentRepository.delete(e);
	}

	@Transactional(readOnly = true)
	public Map<String, Object> membershipOverview(Integer clientId) {
		requireClient(clientId);
		MembershipPurchase purchase = findCurrentPurchase(clientId);
		Map<String, Object> out = new LinkedHashMap<>();
		if (purchase == null) {
			out.put("membership", null);
			out.put("rules", List.of());
			out.put("is_frozen", false);
			return out;
		}
		out.put("membership", membershipPurchaseRow(purchase));
		out.put("rules", membershipRulesForPurchase(purchase));
		out.put("is_frozen", purchase.getFreezeStartedOn() != null);
		out.put("freeze_started_on", purchase.getFreezeStartedOn());
		out.put("freeze_requested_days", purchase.getFreezeRequestedDays());
		return out;
	}

	@Transactional
	public Map<String, Object> freezeMembership(Integer clientId, Integer days) {
		if (days == null) {
			throw ApiValidationException.single("days", "Укажите количество дней заморозки.");
		}
		MembershipPurchase purchase = requireCurrentPurchase(clientId);
		if (purchase.getFreezeStartedOn() != null) {
			throw ApiValidationException.single("membership", "Заморозка уже активна. Сначала выполните разморозку.");
		}
		int maxDays = purchase.getRemainingFreezeDays() == null ? 0 : purchase.getRemainingFreezeDays();
		if (days < 1 || days > maxDays) {
			throw ApiValidationException.single("days", "Число дней должно быть в интервале от 1 до " + maxDays + ".");
		}
		purchase.setFreezeStartedOn(LocalDate.now());
		purchase.setFreezeRequestedDays(days);
		return membershipOverview(clientId);
	}

	@Transactional
	public Map<String, Object> unfreezeMembership(Integer clientId) {
		MembershipPurchase purchase = requireCurrentPurchase(clientId);
		if (purchase.getFreezeStartedOn() == null) {
			throw ApiValidationException.single("membership", "У абонемента нет активной заморозки.");
		}
		long usedLong = ChronoUnit.DAYS.between(purchase.getFreezeStartedOn(), LocalDate.now());
		int usedDays = (int) Math.max(0L, usedLong);
		int requestedDays = purchase.getFreezeRequestedDays() == null ? usedDays : purchase.getFreezeRequestedDays();
		usedDays = Math.min(usedDays, requestedDays);
		int remaining = purchase.getRemainingFreezeDays() == null ? 0 : purchase.getRemainingFreezeDays();
		purchase.setRemainingFreezeDays(Math.max(0, remaining - usedDays));
		purchase.setEndDate(purchase.getEndDate().plusDays(usedDays));
		purchase.setFreezeStartedOn(null);
		purchase.setFreezeRequestedDays(null);
		return membershipOverview(clientId);
	}

	@Transactional
	public void annulMembership(Integer clientId) {
		MembershipPurchase purchase = requireCurrentPurchase(clientId);
		membershipPurchaseRepository.delete(purchase);
	}

	private void applyPersonalFromRequest(PersonalData pd, String last, String first, String middle,
			java.time.LocalDate birth, String phone, String email, com.fitnessclub.domain.Gender gender,
			String regAddr, String actAddr, String passwordHash) {
		pd.setLastName(last);
		pd.setFirstName(first);
		pd.setMiddleName(middle == null || middle.isBlank() ? null
				: TextNormalizationUtils.capitalizeCyrillicNamePart(middle.trim()));
		pd.setBirthDate(birth);
		pd.setPhone(phone);
		pd.setEmail(email);
		pd.setGender(gender);
		pd.setRegistrationAddress(regAddr);
		pd.setActualAddress(actAddr);
		pd.setPasswordHash(passwordHash);
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

	private Client requireClient(Integer clientId) {
		return clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
	}

	private MembershipPurchase findCurrentPurchase(Integer clientId) {
		List<MembershipPurchase> purchases = membershipPurchaseRepository.findCurrentManageableByClientId(clientId);
		if (purchases.isEmpty()) {
			return null;
		}
		return purchases.get(0);
	}

	private MembershipPurchase requireCurrentPurchase(Integer clientId) {
		requireClient(clientId);
		MembershipPurchase purchase = findCurrentPurchase(clientId);
		if (purchase == null) {
			throw ApiValidationException.single("membership", "У клиента нет активного абонемента.");
		}
		return purchase;
	}

	private Map<String, Object> membershipPurchaseRow(MembershipPurchase p) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("purchase_id", p.getPurchaseId());
		x.put("membership_name", p.getMembership().getMembershipName());
		x.put("remaining_freeze_days", p.getRemainingFreezeDays());
		x.put("remaining_guest_visits", p.getRemainingGuestVisits());
		x.put("start_date", p.getStartDate());
		x.put("end_date", p.getEndDate());
		x.put("status", p.getFreezeStartedOn() != null ? "заморожен" : p.getStatus());
		return x;
	}

	private List<Map<String, Object>> membershipRulesForPurchase(MembershipPurchase p) {
		List<MembershipServiceLink> links = membershipServiceLinkRepository.findByMembershipIdOrderByServiceIdAsc(
				p.getMembership().getMembershipId());
		return links.stream().map(ms -> {
			FitnessServiceEntity service = fitnessServiceRepository.findById(ms.getServiceId()).orElse(null);
			long used = membershipUsageRecordRepository.countByMembershipPurchase_PurchaseIdAndService_ServiceId(
					p.getPurchaseId(), ms.getServiceId());
			Integer included = ms.getIncludedVisits();
			Integer rem = included == null ? null : Math.max(0, included - (int) used);
			Map<String, Object> x = new LinkedHashMap<>();
			x.put("service_name", service == null ? "—" : service.getServiceName());
			x.put("included_visits", included == null ? "безлимит" : included);
			x.put("remaining", rem == null ? "—" : rem);
			x.put("max_per_week", ms.getMaxPerWeek());
			x.put("days_of_week", ms.getDaysOfWeek());
			x.put("time_start", ms.getTimeStart());
			x.put("time_end", ms.getTimeEnd());
			return x;
		}).collect(Collectors.toList());
	}

	private ClientResponseDto toDto(Client c) {
		PersonalData p = c.getPersonalData();
		return ClientResponseDto.builder()
				.clientId(c.getClientId())
				.personalDataId(p.getPersonalDataId())
				.lastName(p.getLastName())
				.firstName(p.getFirstName())
				.middleName(p.getMiddleName())
				.birthDate(p.getBirthDate())
				.phone(p.getPhone())
				.email(p.getEmail())
				.gender(p.getGender())
				.isActive(Boolean.TRUE.equals(c.getIsActive()))
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
