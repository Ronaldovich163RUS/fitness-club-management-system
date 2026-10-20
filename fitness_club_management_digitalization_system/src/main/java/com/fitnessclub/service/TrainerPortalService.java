package com.fitnessclub.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.Gender;
import com.fitnessclub.domain.Hall;
import com.fitnessclub.domain.InAppNotification;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.ScheduleSlot;
import com.fitnessclub.domain.ServicePurchaseEntity;
import com.fitnessclub.domain.Trainer;
import com.fitnessclub.domain.TrainerZone;
import com.fitnessclub.domain.TrainingPlanEntity;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.HallRepository;
import com.fitnessclub.repository.InAppNotificationRepository;
import com.fitnessclub.repository.PersonalDataRepository;
import com.fitnessclub.repository.ScheduleSlotRepository;
import com.fitnessclub.repository.ServicePurchaseEntityRepository;
import com.fitnessclub.repository.TrainerRepository;
import com.fitnessclub.repository.TrainerZoneRepository;
import com.fitnessclub.repository.TrainingPlanRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrainerPortalService {

	private static final Logger log = LoggerFactory.getLogger(TrainerPortalService.class);

	private final TrainerRepository trainerRepository;
	private final PersonalDataRulesService personalDataRulesService;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final FitnessServiceRepository fitnessServiceRepository;
	private final HallRepository hallRepository;
	private final TrainerZoneRepository trainerZoneRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final ClientRepository clientRepository;
	private final InAppNotificationRepository inAppNotificationRepository;
	private final ServicePurchaseEntityRepository servicePurchaseEntityRepository;
	private final PersonalDataRepository personalDataRepository;

	@Transactional(readOnly = true)
	public Map<String, Object> profile(HttpSession session) {
		Trainer trainer = requireTrainer(session);
		PersonalData p = trainer.getPersonalData();
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("trainer_id", trainer.getTrainerId());
		out.put("personal_data_id", p.getPersonalDataId());
		out.put("last_name", p.getLastName());
		out.put("first_name", p.getFirstName());
		out.put("middle_name", p.getMiddleName());
		out.put("birth_date", p.getBirthDate());
		out.put("phone", p.getPhone());
		out.put("email", p.getEmail());
		out.put("gender", p.getGender() == null ? null : p.getGender().name());
		out.put("registration_address", p.getRegistrationAddress());
		out.put("actual_address", p.getActualAddress());
		return out;
	}

	@Transactional
	public void updateProfile(Map<String, Object> body, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		PersonalData p = trainer.getPersonalData();

		Map<String, String> errors = new LinkedHashMap<>();
		String lastName = str(body, "last_name");
		String firstName = str(body, "first_name");
		String middleName = str(body, "middle_name");
		LocalDate birthDate = parseDate(body.get("birth_date"));
		String phoneRaw = str(body, "phone");
		String email = str(body, "email");
		Gender gender = parseGender(body.get("gender"));

		personalDataRulesService.validatePersonNames(lastName, firstName, middleName, errors);
		personalDataRulesService.validateBirthDate(birthDate, 18, errors, "birth_date");
		personalDataRulesService.validatePhoneRaw(phoneRaw, errors);
		personalDataRulesService.validateEmailFormat(email, errors);
		personalDataRulesService.validateGender(gender, errors);
		if (!errors.isEmpty()) throw new ApiValidationException(errors);

		String phoneNormalized = "+" + personalDataRulesService.requireNormalizedPhone(phoneRaw);
		if (personalDataRepository.existsByEmailIgnoreCaseAndPersonalDataIdNot(email, p.getPersonalDataId())) {
			throw ApiValidationException.single("email", "Пользователь с таким email уже существует.");
		}
		if (personalDataRepository.existsByPhoneAndPersonalDataIdNot(phoneNormalized, p.getPersonalDataId())) {
			throw ApiValidationException.single("phone", "Пользователь с таким телефоном уже существует.");
		}

		p.setLastName(lastName);
		p.setFirstName(firstName);
		p.setMiddleName(blankToNull(middleName));
		p.setBirthDate(birthDate);
		p.setPhone(phoneNormalized);
		p.setEmail(email == null ? null : email.trim().toLowerCase());
		p.setGender(gender);
		p.setRegistrationAddress(blankToNull(str(body, "registration_address")));
		p.setActualAddress(blankToNull(str(body, "actual_address")));
	}

	@Transactional(readOnly = true)
	public Map<String, Object> slotsMeta(HttpSession session) {
		Trainer trainer = requireTrainer(session);
		Set<Integer> allowedZones = trainerZoneRepository.findByTrainerId(trainer.getTrainerId()).stream()
				.map(TrainerZone::getZoneId)
				.collect(Collectors.toSet());
		boolean hasZoneRestrictions = !allowedZones.isEmpty();
		List<FitnessServiceEntity> activePersonalServices = fitnessServiceRepository.findAllFetchZone().stream()
				.filter(s -> Boolean.TRUE.equals(s.getIsActive()))
				.filter(s -> "персональная тренировка".equals(normalizeType(s.getServiceType())))
				.filter(s -> s.getZone() != null)
				.collect(Collectors.toList());
		Set<Integer> effectiveZoneIds = activePersonalServices.stream()
				.map(s -> s.getZone().getZoneId())
				.filter(id -> !hasZoneRestrictions || allowedZones.contains(id))
				.collect(Collectors.toSet());
		List<Map<String, Object>> zones = activePersonalServices.stream()
				.map(FitnessServiceEntity::getZone)
				.filter(z -> z != null)
				.filter(z -> effectiveZoneIds.contains(z.getZoneId()))
				.collect(Collectors.toMap(
						z -> z.getZoneId(),
						z -> z,
						(a, b) -> a,
						LinkedHashMap::new))
				.values()
				.stream()
				.map(z -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("zone_id", z.getZoneId());
					row.put("zone_name", z.getZoneName());
					return row;
				})
				.collect(Collectors.toList());
		List<Map<String, Object>> halls = hallRepository.findAllFetchZone().stream()
				.filter(h -> !Boolean.FALSE.equals(h.getIsActive()))
				.filter(h -> h.getZone() != null)
				.filter(h -> effectiveZoneIds.contains(h.getZone().getZoneId()))
				.map(h -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("hall_id", h.getHallId());
					row.put("hall_name", h.getHallName());
					row.put("zone_id", h.getZone().getZoneId());
					row.put("zone_name", h.getZone().getZoneName());
					return row;
				})
				.collect(Collectors.toList());
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("zones", zones);
		out.put("halls", halls);
		return out;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> slots(HttpSession session, boolean personalOnly) {
		Trainer trainer = requireTrainer(session);
		List<ScheduleSlot> list = personalOnly
				? scheduleSlotRepository.findPersonalByTrainerIdFetchRelations(trainer.getTrainerId())
				: scheduleSlotRepository.findByTrainerIdFetchRelations(trainer.getTrainerId());
		return list.stream().map(this::slotRow).collect(Collectors.toList());
	}

	@Transactional
	public void createSlot(Map<String, Object> body, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		Integer hallId = intVal(body.get("hall_id"));
		LocalDate date = parseDate(body.get("date"));
		LocalTime time = parseTime(body.get("time"));
		if (hallId == null || date == null || time == null) {
			throw ApiValidationException.single("slot", "Укажите дату, время и зал.");
		}

		Hall hall = hallRepository.findById(hallId)
				.orElseThrow(() -> ApiValidationException.single("hall_id", "Зал не найден."));
		if (Boolean.FALSE.equals(hall.getIsActive())) {
			throw ApiValidationException.single("hall_id", "Зал неактивен.");
		}

		Set<Integer> allowedZones = trainerZoneRepository.findByTrainerId(trainer.getTrainerId()).stream()
				.map(TrainerZone::getZoneId)
				.collect(Collectors.toSet());
		boolean hasZoneRestrictions = !allowedZones.isEmpty();
		Integer hallZoneId = hall.getZone() == null ? null : hall.getZone().getZoneId();
		if (hallZoneId == null) {
			throw ApiValidationException.single("hall_id", "Для выбранного зала не назначена зона.");
		}
		if (hasZoneRestrictions && !allowedZones.contains(hallZoneId)) {
			throw ApiValidationException.single("hall_id", "Выбранный зал недоступен для тренера.");
		}
		FitnessServiceEntity personalService = fitnessServiceRepository.findActivePersonalByZoneId(hallZoneId).stream()
				.findFirst()
				.orElseThrow(() -> ApiValidationException.single("hall_id",
						"Для выбранной зоны отсутствует активная услуга типа «персональная тренировка»."));

		LocalDateTime start = LocalDateTime.of(date, time);
		LocalDateTime end = start.plusMinutes(60);
		LocalTime workStart = LocalTime.of(7, 0);
		LocalTime workEnd = LocalTime.of(22, 0);
		if (start.toLocalTime().isBefore(workStart) || end.toLocalTime().isAfter(workEnd)) {
			throw ApiValidationException.single("time", "Персональную тренировку можно создать только в интервале 07:00-22:00.");
		}
		if (!start.isAfter(LocalDateTime.now())) {
			throw ApiValidationException.single("date", "Слот должен быть в будущем.");
		}
		if (scheduleSlotRepository.countActiveTooCloseForTrainer(trainer.getTrainerId(), start, end) > 0) {
			throw ApiValidationException.single("time", "У тренера уже есть занятие в это время.");
		}
		if (scheduleSlotRepository.countActiveTooCloseInHall(hall.getHallId(), start, end) > 0) {
			throw ApiValidationException.single("hall_id", "Зал занят в это время.");
		}

		ScheduleSlot slot = new ScheduleSlot();
		slot.setService(personalService);
		slot.setTrainer(trainer);
		slot.setHall(hall);
		slot.setStartDatetime(start);
		slot.setEndDatetime(end);
		slot.setMaxParticipants(1);
		slot.setCurrentParticipants(0);
		slot.setIsCancelled(Boolean.FALSE);
		scheduleSlotRepository.save(slot);
	}

	@Transactional
	public void cancelSlot(Integer scheduleId, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		ScheduleSlot slot = scheduleSlotRepository.findByIdFetchRelations(scheduleId)
				.orElseThrow(() -> ApiValidationException.single("schedule_id", "Слот не найден."));
		if (!slot.getTrainer().getTrainerId().equals(trainer.getTrainerId())) {
			throw ApiValidationException.single("schedule_id", "Слот не принадлежит тренеру.");
		}
		if (!"персональная тренировка".equals(normalizeType(slot.getService().getServiceType()))) {
			throw ApiValidationException.single("schedule_id", "Можно отменить только персональную тренировку.");
		}
		slot.setIsCancelled(Boolean.TRUE);
		slot.setCurrentParticipants(0);
		servicePurchaseEntityRepository.findBySchedule_ScheduleId(scheduleId).forEach(p -> p.setIsUsed(Boolean.TRUE));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> assignedClients(HttpSession session) {
		Trainer trainer = requireTrainer(session);
		List<com.fitnessclub.domain.ServicePurchaseEntity> purchases = servicePurchaseEntityRepository
				.findByTrainerSchedule(trainer.getTrainerId());
		Set<Integer> ids = purchases.stream()
				.filter(p -> p.getService() != null)
				.filter(p -> "персональная тренировка".equals(normalizeType(p.getService().getServiceType())))
				.map(p -> p.getClient() == null ? null : p.getClient().getClientId())
				.filter(id -> id != null)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		log.info("trainer.assignedClients debug: trainerId={}, totalPurchasesByTrainerSchedule={}, personalClientIdsCount={}, personalClientIds={}",
				trainer.getTrainerId(), purchases.size(), ids.size(), ids);

		List<Map<String, Object>> out = new ArrayList<>();
		for (Integer id : ids) {
			Client c = clientRepository.findByIdFetchPersonal(id).orElse(null);
			if (c == null) continue;
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("client_id", c.getClientId());
			row.put("full_name", fullName(c.getPersonalData()));
			out.add(row);
		}
		return out;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> plans(HttpSession session) {
		Trainer trainer = requireTrainer(session);
		return trainingPlanRepository.findByTrainerIdFetchAll(trainer.getTrainerId()).stream()
				.map(this::planRow)
				.collect(Collectors.toList());
	}

	@Transactional
	public void createPlan(Map<String, Object> body, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		Client client = requireAssignedClient(trainer.getTrainerId(), intVal(body.get("client_id")));
		if (trainingPlanRepository.existsByTrainer_TrainerIdAndClient_ClientId(trainer.getTrainerId(), client.getClientId())) {
			throw ApiValidationException.single("client_id",
					"Для выбранного клиента уже создан план. Доступно только редактирование.");
		}
		String name = str(body, "plan_name");
		String exercises = str(body, "exercises");
		if (isBlank(name) || isBlank(exercises)) {
			throw ApiValidationException.single("plan", "Заполните название и упражнения.");
		}
		TrainingPlanEntity plan = new TrainingPlanEntity();
		plan.setTrainer(trainer);
		plan.setClient(client);
		plan.setPlanName(name);
		plan.setDescription(blankToNull(str(body, "description")));
		plan.setExercises(exercises);
		trainingPlanRepository.save(plan);
	}

	@Transactional
	public void updatePlan(Integer planId, Map<String, Object> body, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		TrainingPlanEntity plan = trainingPlanRepository.findByIdFetchAll(planId)
				.orElseThrow(() -> ApiValidationException.single("plan_id", "План не найден."));
		if (!plan.getTrainer().getTrainerId().equals(trainer.getTrainerId())) {
			throw ApiValidationException.single("plan_id", "План не принадлежит тренеру.");
		}
		String name = str(body, "plan_name");
		String exercises = str(body, "exercises");
		if (isBlank(name) || isBlank(exercises)) {
			throw ApiValidationException.single("plan", "Заполните название и упражнения.");
		}
		plan.setPlanName(name);
		plan.setDescription(blankToNull(str(body, "description")));
		plan.setExercises(exercises);
	}

	@Transactional
	public void deletePlan(Integer planId, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		TrainingPlanEntity plan = trainingPlanRepository.findByIdFetchAll(planId)
				.orElseThrow(() -> ApiValidationException.single("plan_id", "План не найден."));
		if (!plan.getTrainer().getTrainerId().equals(trainer.getTrainerId())) {
			throw ApiValidationException.single("plan_id", "План не принадлежит тренеру.");
		}
		trainingPlanRepository.delete(plan);
	}

	@Transactional
	public void sendNotification(Map<String, Object> body, HttpSession session) {
		Trainer trainer = requireTrainer(session);
		Client client = requireAssignedClient(trainer.getTrainerId(), intVal(body.get("client_id")));
		String title = str(body, "title");
		String message = str(body, "message");
		if (isBlank(title) || isBlank(message)) {
			throw ApiValidationException.single("notification", "Заполните заголовок и сообщение.");
		}
		InAppNotification n = new InAppNotification();
		n.setRecipientType("client");
		n.setRecipientClient(client);
		n.setRecipientTrainer(null);
		n.setSenderAdminId(null);
		n.setSenderTrainer(trainer);
		n.setNotificationType("system");
		n.setTitle(title);
		n.setMessage(message);
		n.setIsSent(Boolean.TRUE);
		n.setSentDatetime(LocalDateTime.now());
		inAppNotificationRepository.save(n);
	}

	private Client requireAssignedClient(Integer trainerId, Integer clientId) {
		if (clientId == null) {
			throw ApiValidationException.single("client_id", "Выберите клиента.");
		}
		List<com.fitnessclub.domain.ServicePurchaseEntity> purchases = servicePurchaseEntityRepository
				.findByTrainerSchedule(trainerId);
		Set<Integer> ids = purchases.stream()
				.filter(p -> p.getService() != null)
				.filter(p -> "персональная тренировка".equals(normalizeType(p.getService().getServiceType())))
				.map(p -> p.getClient() == null ? null : p.getClient().getClientId())
				.filter(id -> id != null)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		log.info("trainer.requireAssignedClient debug: trainerId={}, requestedClientId={}, totalPurchasesByTrainerSchedule={}, personalClientIdsCount={}, personalClientIds={}",
				trainerId, clientId, purchases.size(), ids.size(), ids);
		if (!ids.contains(clientId)) {
			throw ApiValidationException.single("client_id", "Клиент не закреплён за тренером.");
		}
		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		return client;
	}

	private Map<String, Object> planRow(TrainingPlanEntity p) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("plan_id", p.getPlanId());
		row.put("client_id", p.getClient().getClientId());
		row.put("client_name", fullName(p.getClient().getPersonalData()));
		row.put("trainer_id", p.getTrainer().getTrainerId());
		row.put("plan_name", p.getPlanName());
		row.put("description", p.getDescription());
		row.put("exercises", p.getExercises());
		row.put("created_at", p.getCreatedAt());
		row.put("updated_at", p.getUpdatedAt());
		return row;
	}

	private Map<String, Object> slotRow(ScheduleSlot s) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("schedule_id", s.getScheduleId());
		row.put("service_id", s.getService().getServiceId());
		row.put("service_name", s.getService().getServiceName());
		row.put("service_type", s.getService().getServiceType());
		row.put("hall_id", s.getHall().getHallId());
		row.put("hall_name", s.getHall().getHallName());
		row.put("zone_id", s.getHall().getZone() == null ? null : s.getHall().getZone().getZoneId());
		row.put("zone_name", s.getHall().getZone() == null ? null : s.getHall().getZone().getZoneName());
		row.put("start_datetime", s.getStartDatetime());
		row.put("end_datetime", s.getEndDatetime());
		row.put("max_participants", s.getMaxParticipants());
		row.put("current_participants", s.getCurrentParticipants());
		row.put("is_cancelled", Boolean.TRUE.equals(s.getIsCancelled()));
		row.put("client_full_name", bookedClientFullNameForSlot(s.getScheduleId()));
		return row;
	}

	/** ФИО клиента с активной (не использованной) записью на слот; иначе пустая строка. */
	private String bookedClientFullNameForSlot(Integer scheduleId) {
		if (scheduleId == null) {
			return "";
		}
		List<ServicePurchaseEntity> purchases = servicePurchaseEntityRepository.findWithClientByScheduleId(scheduleId);
		return purchases.stream()
				.filter(p -> Boolean.FALSE.equals(p.getIsUsed()))
				.filter(p -> p.getClient() != null && p.getClient().getPersonalData() != null)
				.findFirst()
				.map(p -> fullName(p.getClient().getPersonalData()))
				.orElse("");
	}

	private Trainer requireTrainer(HttpSession session) {
		Object role = session.getAttribute("auth_role");
		if (role == null || !"TRAINER".equals(String.valueOf(role))) {
			throw ApiValidationException.single("auth", "Требуется роль тренера.");
		}
		Object profile = session.getAttribute("auth_profile_id");
		if (profile == null) {
			throw ApiValidationException.single("auth", "Пользователь не авторизован.");
		}
		Integer pdId = Integer.valueOf(String.valueOf(profile));
		return trainerRepository.findAllWithPersonalData().stream()
				.filter(t -> t.getPersonalData() != null && pdId.equals(t.getPersonalData().getPersonalDataId()))
				.findFirst()
				.orElseThrow(() -> ApiValidationException.single("auth", "Тренер не найден."));
	}

	private static String fullName(PersonalData p) {
		String mn = p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : " " + p.getMiddleName();
		return (p.getLastName() + " " + p.getFirstName() + mn).trim();
	}

	private static LocalDate parseDate(Object raw) {
		try {
			return raw == null ? null : LocalDate.parse(String.valueOf(raw));
		} catch (Exception ex) {
			return null;
		}
	}

	private static LocalTime parseTime(Object raw) {
		try {
			return raw == null ? null : LocalTime.parse(String.valueOf(raw));
		} catch (Exception ex) {
			return null;
		}
	}

	private static Gender parseGender(Object raw) {
		try {
			return raw == null ? null : Gender.valueOf(String.valueOf(raw));
		} catch (Exception ex) {
			return null;
		}
	}

	private static Integer intVal(Object raw) {
		try {
			return raw == null ? null : Integer.valueOf(String.valueOf(raw));
		} catch (Exception ex) {
			return null;
		}
	}

	private static String str(Map<String, Object> body, String key) {
		Object v = body.get(key);
		return v == null ? null : String.valueOf(v).trim();
	}

	private static String normalizeType(String serviceType) {
		return serviceType == null ? "" : serviceType.trim().toLowerCase();
	}

	private static boolean isBlank(String x) {
		return x == null || x.isBlank();
	}

	private static String blankToNull(String x) {
		return isBlank(x) ? null : x.trim();
	}
}
