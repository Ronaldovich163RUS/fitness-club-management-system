package com.fitnessclub.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.DocumentEntity;
import com.fitnessclub.domain.DocumentType;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.Gender;
import com.fitnessclub.domain.InAppNotification;
import com.fitnessclub.domain.MembershipPurchase;
import com.fitnessclub.domain.MembershipServiceLink;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.ScheduleSlot;
import com.fitnessclub.domain.ServicePurchaseEntity;
import com.fitnessclub.domain.Trainer;
import com.fitnessclub.domain.Zone;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.ClubStatsRepository;
import com.fitnessclub.repository.DocumentRepository;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.InAppNotificationRepository;
import com.fitnessclub.repository.MembershipPlanRepository;
import com.fitnessclub.repository.MembershipPurchaseRepository;
import com.fitnessclub.repository.MembershipServiceLinkRepository;
import com.fitnessclub.repository.MembershipUsageRecordRepository;
import com.fitnessclub.repository.ScheduleSlotRepository;
import com.fitnessclub.repository.ServicePurchaseEntityRepository;
import com.fitnessclub.repository.TrainerRepository;
import com.fitnessclub.repository.TrainingPlanRepository;
import com.fitnessclub.repository.ZoneRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClientPortalService {

	/** Как в колонке БД (без пересчёта UTC → пояс). */
	private static final DateTimeFormatter PURCHASE_DATE_DISPLAY = DateTimeFormatter
			.ofPattern("dd.MM.yyyy, HH:mm")
			.withLocale(Locale.forLanguageTag("ru-RU"));

	private final ClientRepository clientRepository;
	private final FitnessServiceRepository serviceRepository;
	private final MembershipPlanRepository membershipPlanRepository;
	private final MembershipServiceLinkRepository membershipServiceLinkRepository;
	private final MembershipPurchaseRepository membershipPurchaseRepository;
	private final ServicePurchaseEntityRepository servicePurchaseRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final TrainerRepository trainerRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final ZoneRepository zoneRepository;
	private final InAppNotificationRepository inAppNotificationRepository;
	private final ClubStatsRepository clubStatsRepository;
	private final DocumentRepository documentRepository;
	private final MembershipUsageRecordRepository membershipUsageRecordRepository;

	@Transactional(readOnly = true)
	public List<Map<String, Object>> trainingTypes() {
		return serviceRepository.findAllFetchZone().stream()
				.filter(s -> Boolean.TRUE.equals(s.getIsActive()))
				.filter(s -> isTrainingType(s.getServiceType()))
				.map(this::serviceCard)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> trainers() {
		return trainerRepository.findActiveWithPersonalOrderByName().stream().map(this::trainerCard).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> scheduleGroup() {
		return scheduleSlotRepository.findAllFetchRelations().stream()
				.filter(s -> !Boolean.TRUE.equals(s.getIsCancelled()))
				.filter(s -> isGroupType(s.getService().getServiceType()))
				.filter(this::trainerSlotIsConductable)
				.map(this::scheduleRow)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> membershipCatalog() {
		return membershipPlanRepository.findByIsActiveTrueOrderByMembershipNameAsc().stream()
				.map(m -> {
					Map<String, Object> x = new LinkedHashMap<>();
					x.put("membership_id", m.getMembershipId());
					x.put("membership_name", m.getMembershipName());
					x.put("description", m.getDescription());
					x.put("price", m.getPrice());
					x.put("duration_days", m.getDurationDays());
					x.put("guest_visits", m.getGuestVisits());
					x.put("freeze_days", m.getFreezeDays());
					x.put("towel", Boolean.TRUE.equals(m.getTowel()));
					x.put("parking", Boolean.TRUE.equals(m.getParking()));
					return x;
				}).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> membershipRules(Integer membershipId) {
		return membershipServiceLinkRepository.findByMembershipIdOrderByServiceIdAsc(membershipId).stream()
				.map(ms -> {
					FitnessServiceEntity s = serviceRepository.findById(ms.getServiceId()).orElse(null);
					Map<String, Object> x = new LinkedHashMap<>();
					x.put("service_name", s == null ? "—" : s.getServiceName());
					x.put("included_visits", ms.getIncludedVisits());
					x.put("remaining", ms.getIncludedVisits());
					x.put("max_per_week", ms.getMaxPerWeek());
					x.put("days_of_week", ms.getDaysOfWeek());
					x.put("time_start", ms.getTimeStart());
					x.put("time_end", ms.getTimeEnd());
					return x;
				}).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> serviceCatalog() {
		return serviceRepository.findAllFetchZone().stream()
				.filter(s -> Boolean.TRUE.equals(s.getIsActive()))
				.map(this::serviceCard)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Map<String, Object> serviceRulesOneRow(Integer serviceId) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("included_visits", 1);
		x.put("remaining", 1);
		x.put("max_per_week", null);
		x.put("days_of_week", null);
		x.put("time_start", null);
		x.put("time_end", null);
		return x;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> profile(HttpSession session) {
		Client c = requireClient(session);
		PersonalData p = c.getPersonalData();
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("personal_data_id", p.getPersonalDataId());
		x.put("last_name", p.getLastName());
		x.put("first_name", p.getFirstName());
		x.put("middle_name", p.getMiddleName());
		x.put("birth_date", p.getBirthDate());
		x.put("phone", p.getPhone());
		x.put("email", p.getEmail());
		x.put("gender", p.getGender() == null ? null : p.getGender().name());
		x.put("registration_address", p.getRegistrationAddress());
		x.put("actual_address", p.getActualAddress());
		x.put("documents", documentRepository.findByPersonalDataPersonalDataIdOrderByDocumentId(p.getPersonalDataId()).stream()
				.map(this::documentRow).collect(Collectors.toList()));
		return x;
	}

	@Transactional
	public void updateProfile(Map<String, Object> body, HttpSession session) {
		Client c = requireClient(session);
		PersonalData p = c.getPersonalData();
		p.setLastName(str(body, "last_name"));
		p.setFirstName(str(body, "first_name"));
		p.setMiddleName(str(body, "middle_name"));
		p.setPhone(str(body, "phone"));
		p.setRegistrationAddress(str(body, "registration_address"));
		p.setActualAddress(str(body, "actual_address"));
		if (body.get("birth_date") != null) p.setBirthDate(LocalDate.parse(String.valueOf(body.get("birth_date"))));
		if (body.get("gender") != null) p.setGender(Gender.valueOf(String.valueOf(body.get("gender"))));
	}

	@Transactional
	public Map<String, Object> addDocument(Map<String, Object> body, HttpSession session) {
		Client c = requireClient(session);
		DocumentEntity d = new DocumentEntity();
		d.setPersonalData(c.getPersonalData());
		d.setDocumentType(DocumentType.valueOf(String.valueOf(body.get("document_type"))));
		d.setSeries(str(body, "series"));
		d.setNumber(str(body, "number"));
		d.setIssuedBy(str(body, "issued_by"));
		if (body.get("issue_date") != null && !String.valueOf(body.get("issue_date")).isBlank()) d.setIssueDate(LocalDate.parse(String.valueOf(body.get("issue_date"))));
		if (body.get("expiry_date") != null && !String.valueOf(body.get("expiry_date")).isBlank()) d.setExpiryDate(LocalDate.parse(String.valueOf(body.get("expiry_date"))));
		d.setNotes(str(body, "notes"));
		d.setVerified(Boolean.FALSE);
		return documentRow(documentRepository.save(d));
	}

	@Transactional
	public Map<String, Object> updateDocument(Integer id, Map<String, Object> body, HttpSession session) {
		Client c = requireClient(session);
		DocumentEntity d = documentRepository.findByDocumentIdAndPersonalDataPersonalDataId(id, c.getPersonalData().getPersonalDataId())
				.orElseThrow(() -> ApiValidationException.single("document_id", "Документ не найден."));
		d.setDocumentType(DocumentType.valueOf(String.valueOf(body.get("document_type"))));
		d.setSeries(str(body, "series"));
		d.setNumber(str(body, "number"));
		d.setIssuedBy(str(body, "issued_by"));
		d.setIssueDate(body.get("issue_date") == null || String.valueOf(body.get("issue_date")).isBlank()
				? null : LocalDate.parse(String.valueOf(body.get("issue_date"))));
		d.setExpiryDate(body.get("expiry_date") == null || String.valueOf(body.get("expiry_date")).isBlank()
				? null : LocalDate.parse(String.valueOf(body.get("expiry_date"))));
		d.setNotes(str(body, "notes"));
		d.setVerified(Boolean.FALSE);
		return documentRow(d);
	}

	@Transactional
	public void deleteDocument(Integer id, HttpSession session) {
		Client c = requireClient(session);
		DocumentEntity d = documentRepository.findByDocumentIdAndPersonalDataPersonalDataId(id, c.getPersonalData().getPersonalDataId())
				.orElseThrow(() -> ApiValidationException.single("document_id", "Документ не найден."));
		documentRepository.delete(d);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> notifications(HttpSession session) {
		Client c = requireClient(session);
		return inAppNotificationRepository.findAllByOrderBySentDatetimeDesc().stream()
				.filter(n -> "client".equalsIgnoreCase(n.getRecipientType()))
				.filter(n -> n.getRecipientClient() != null && n.getRecipientClient().getClientId().equals(c.getClientId()))
				.map(this::notificationRow)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Map<String, Object> visitors() {
		Integer count = clubStatsRepository.findFirstByOrderByIdAsc().map(s -> s.getCurrentVisitors()).orElse(0);
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("current_visitors", count == null ? 0 : count);
		return x;
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> trainingPlans(HttpSession session) {
		Client client = requireClient(session);
		return trainingPlanRepository.findByClientIdFetchAll(client.getClientId()).stream()
				.map(p -> {
					Map<String, Object> x = new LinkedHashMap<>();
					x.put("plan_id", p.getPlanId());
					x.put("client_id", p.getClient().getClientId());
					x.put("trainer_id", p.getTrainer().getTrainerId());
					x.put("trainer_name", fullName(p.getTrainer().getPersonalData()));
					x.put("plan_name", p.getPlanName());
					x.put("description", p.getDescription());
					x.put("exercises", p.getExercises());
					x.put("created_at", p.getCreatedAt());
					x.put("updated_at", p.getUpdatedAt());
					return x;
				})
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Map<String, Object> membershipsOverview(HttpSession session) {
		Client c = requireClient(session);
		Map<String, Object> out = new LinkedHashMap<>();
		List<MembershipPurchase> activeList = membershipPurchaseRepository.findActiveForClientOnDate(c.getClientId(),
				LocalDate.now(), "активен");
		MembershipPurchase active = activeList.isEmpty() ? null : activeList.get(0);
		out.put("membership", active == null ? null : membershipPurchaseRow(active));
		out.put("rules", active == null ? List.of() : membershipRulesForPurchase(active));
		out.put("service_purchases", servicePurchaseRepository.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(c.getClientId()).stream()
				.map(this::servicePurchaseRuleRow).collect(Collectors.toList()));
		return out;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> personalTrainingData(HttpSession session) {
		Client c = requireClient(session);
		Integer currentPersonalDataId = c.getPersonalData() == null ? null : c.getPersonalData().getPersonalDataId();
		boolean isAlsoTrainer = currentPersonalDataId != null
				&& trainerRepository.existsByPersonalData_PersonalDataId(currentPersonalDataId);
		Set<Integer> allowedZoneIds = availablePersonalTrainingZoneIds(c);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("trainers", trainerRepository.findActiveWithPersonalOrderByName().stream().map(this::trainerMini).collect(Collectors.toList()));
		out.put("zones", zoneRepository.findAll().stream()
				.filter(z -> allowedZoneIds.contains(z.getZoneId()))
				.map(this::zoneRow)
				.collect(Collectors.toList()));
		out.put("slots", scheduleSlotRepository.findAllFetchRelations().stream()
				.filter(s -> !Boolean.TRUE.equals(s.getIsCancelled()))
				.filter(s -> isPersonalType(s.getService().getServiceType()))
				.filter(s -> s.getService() != null && s.getService().getZone() != null
						&& (isAlsoTrainer || allowedZoneIds.contains(s.getService().getZone().getZoneId())))
				.filter(s -> s.getTrainer() != null && s.getTrainer().getPersonalData() != null)
				.filter(s -> currentPersonalDataId == null
						|| !currentPersonalDataId.equals(s.getTrainer().getPersonalData().getPersonalDataId()))
				.filter(s -> s.getStartDatetime() != null && s.getStartDatetime().isAfter(LocalDateTime.now()))
				.filter(s -> {
					int current = s.getCurrentParticipants() == null ? 0 : s.getCurrentParticipants();
					int max = s.getMaxParticipants() == null ? 0 : s.getMaxParticipants();
					return current < max;
				})
				.map(this::scheduleRow)
				.collect(Collectors.toList()));
		out.put("my_registrations", servicePurchaseRepository.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(c.getClientId()).stream()
				.filter(p -> p.getSchedule() != null && isPersonalType(p.getService().getServiceType()))
				.map(this::personalRegistrationRow).collect(Collectors.toList()));
		return out;
	}

	@Transactional
	public void registerPersonal(Integer scheduleId, HttpSession session) {
		Client c = requireClient(session);
		ScheduleSlot s = scheduleSlotRepository.findById(scheduleId)
				.orElseThrow(() -> ApiValidationException.single("schedule_id", "Слот не найден."));
		if (!isPersonalType(s.getService().getServiceType()) || Boolean.TRUE.equals(s.getIsCancelled())) {
			throw ApiValidationException.single("schedule_id", "Слот недоступен.");
		}
		if (s.getStartDatetime() == null || s.getStartDatetime().isBefore(LocalDateTime.now())) {
			throw ApiValidationException.single("schedule_id", "Слот уже начался.");
		}
		Integer zoneId = s.getService() == null || s.getService().getZone() == null ? null : s.getService().getZone().getZoneId();
		if (zoneId == null || !availablePersonalTrainingZoneIds(c).contains(zoneId)) {
			throw ApiValidationException.single("schedule_id",
					"Запись на этот слот недоступна: зона не входит в доступные персональные услуги клиента.");
		}
		int current = s.getCurrentParticipants() == null ? 0 : s.getCurrentParticipants();
		int max = s.getMaxParticipants() == null ? 0 : s.getMaxParticipants();
		if (current >= max) throw ApiValidationException.single("schedule_id", "Нет свободных мест.");
		if (!trainerSlotIsConductable(s)) {
			throw ApiValidationException.single("schedule_id", "Запись на это занятие недоступна: тренер не активен.");
		}
		boolean already = servicePurchaseRepository.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(c.getClientId()).stream()
				.anyMatch(p -> p.getSchedule() != null && p.getSchedule().getScheduleId().equals(scheduleId));
		if (already) return;
		requireVerifiedPassport(c);
		ServicePurchaseEntity p = new ServicePurchaseEntity();
		p.setClient(c);
		p.setService(s.getService());
		p.setSchedule(s);
		p.setIsUsed(Boolean.FALSE);
		p.setPrice(s.getService().getBasePrice());
		p.setSoldByAdminId(null);
		servicePurchaseRepository.save(p);
		s.setCurrentParticipants(current + 1);
	}

	@Transactional
	public void cancelPersonal(Integer purchaseId, HttpSession session) {
		Client c = requireClient(session);
		ServicePurchaseEntity p = servicePurchaseRepository.findById(purchaseId)
				.orElseThrow(() -> ApiValidationException.single("purchase_id", "Запись не найдена."));
		if (!p.getClient().getClientId().equals(c.getClientId()) || p.getSchedule() == null) {
			throw ApiValidationException.single("purchase_id", "Запись не найдена.");
		}
		ScheduleSlot s = p.getSchedule();
		s.setCurrentParticipants(Math.max(0, (s.getCurrentParticipants() == null ? 0 : s.getCurrentParticipants()) - 1));
		p.setIsUsed(Boolean.TRUE);
	}

	@Transactional
	public void buyMembership(Integer membershipId, HttpSession session) {
		Client c = requireClient(session);
		var m = membershipPlanRepository.findById(membershipId)
				.orElseThrow(() -> ApiValidationException.single("membership_id", "Абонемент не найден."));
		if (!Boolean.TRUE.equals(m.getIsActive())) {
			throw ApiValidationException.single("membership_id", "Абонемент неактивен.");
		}
		if (!membershipPurchaseRepository.findActiveForClientOnDate(c.getClientId(), LocalDate.now(), "активен").isEmpty()) {
			throw ApiValidationException.single("membership_id",
					"У вас уже есть действующий абонемент (в том числе в период заморозки). Второй оформить нельзя до окончания срока или смены статуса на «истёк»/«отменён».");
		}
		requireVerifiedPassport(c);
		MembershipPurchase p = new MembershipPurchase();
		p.setClient(c);
		p.setMembership(m);
		p.setStartDate(LocalDate.now());
		p.setEndDate(LocalDate.now().plusDays(m.getDurationDays() == null ? 0 : m.getDurationDays()));
		p.setStatus("активен");
		p.setRemainingGuestVisits(m.getGuestVisits() == null ? 0 : m.getGuestVisits());
		p.setRemainingFreezeDays(m.getFreezeDays() == null ? 0 : m.getFreezeDays());
		p.setPrice(m.getPrice());
		p.setSoldByAdminId(1);
		membershipPurchaseRepository.save(p);
	}

	@Transactional
	public void buyService(Integer serviceId, HttpSession session) {
		Client c = requireClient(session);
		var s = serviceRepository.findById(serviceId)
				.orElseThrow(() -> ApiValidationException.single("service_id", "Услуга не найдена."));
		if (!Boolean.TRUE.equals(s.getIsActive())) {
			throw ApiValidationException.single("service_id", "Услуга неактивна.");
		}
		requireVerifiedPassport(c);
		ServicePurchaseEntity p = new ServicePurchaseEntity();
		p.setClient(c);
		p.setService(s);
		p.setSchedule(null);
		p.setIsUsed(Boolean.FALSE);
		p.setPrice(s.getBasePrice());
		p.setSoldByAdminId(1);
		servicePurchaseRepository.save(p);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> purchaseHistory(HttpSession session) {
		Client c = requireClient(session);
		List<SimpleEntry<LocalDateTime, LinkedHashMap<String, Object>>> rows = new ArrayList<>();
		for (MembershipPurchase p : membershipPurchaseRepository.findHistoryWithMembership(c.getClientId())) {
			LinkedHashMap<String, Object> x = new LinkedHashMap<>();
			x.put("type", "Абонемент");
			x.put("name", p.getMembership().getMembershipName());
			x.put("price", p.getMembership().getPrice());
			x.put("status", p.getStatus());
			rows.add(new SimpleEntry<>(p.getPurchaseDate(), x));
		}
		for (ServicePurchaseEntity p : servicePurchaseRepository.findHistoryWithService(c.getClientId())) {
			LinkedHashMap<String, Object> x = new LinkedHashMap<>();
			x.put("type", "Услуга");
			x.put("name", p.getService().getServiceName());
			x.put("price", p.getService().getBasePrice());
			x.put("status", Boolean.TRUE.equals(p.getIsUsed()) ? "Использовано" : "Оплачено");
			rows.add(new SimpleEntry<>(p.getPurchaseDate(), x));
		}
		rows.sort((e1, e2) -> {
			LocalDateTime a = e1.getKey();
			LocalDateTime b = e2.getKey();
			if (a == null && b == null) {
				return 0;
			}
			if (a == null) {
				return 1;
			}
			if (b == null) {
				return -1;
			}
			return b.compareTo(a);
		});
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (SimpleEntry<LocalDateTime, LinkedHashMap<String, Object>> e : rows) {
			LocalDateTime when = e.getKey();
			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("date", when == null ? null : PURCHASE_DATE_DISPLAY.format(when));
			line.putAll(e.getValue());
			out.add(line);
		}
		return out;
	}

	/** Паспорт РФ с отметкой «Проверен» в {@code documents} — для покупки абонемента, разовой услуги и записи на персональную тренировку. */
	private void requireVerifiedPassport(Client c) {
		Integer personalDataId = c.getPersonalData().getPersonalDataId();
		if (!documentRepository.existsByPersonalDataPersonalDataIdAndDocumentTypeAndVerifiedTrue(personalDataId,
				DocumentType.PASSPORT_RF)) {
			throw ApiValidationException.single("passport",
					"Покупка доступна только при добавленном паспорте РФ и отметке администратора «Проверен» в разделе документов профиля.");
		}
	}

	private Client requireClient(HttpSession session) {
		Object role = session.getAttribute("auth_role");
		if (role == null || !"CLIENT".equals(String.valueOf(role))) {
			throw ApiValidationException.single("auth", "Требуется роль клиента.");
		}
		Object profile = session.getAttribute("auth_profile_id");
		if (profile == null) {
			throw ApiValidationException.single("auth", "Пользователь не авторизован.");
		}
		Integer pdId = Integer.valueOf(String.valueOf(profile));
		return clientRepository.findByPersonalData_PersonalDataId(pdId)
				.orElseThrow(() -> ApiValidationException.single("auth", "Клиент не найден."));
	}

	private static boolean isTrainingType(String t) {
		String x = t == null ? "" : t.trim().toLowerCase();
		return "групповая тренировка".equals(x) || "персональная тренировка".equals(x);
	}

	private static boolean isGroupType(String t) {
		return "групповая тренировка".equals((t == null ? "" : t.trim().toLowerCase()));
	}

	private static boolean isPersonalType(String t) {
		return "персональная тренировка".equals((t == null ? "" : t.trim().toLowerCase()));
	}

	/** Неактивный тренер не ведёт групповые и персональные занятия в клиентской части. */
	private boolean trainerSlotIsConductable(ScheduleSlot s) {
		if (s == null || s.getTrainer() == null) {
			return false;
		}
		return Boolean.TRUE.equals(s.getTrainer().getIsActive());
	}

	private Map<String, Object> serviceCard(FitnessServiceEntity s) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("service_id", s.getServiceId());
		x.put("service_name", s.getServiceName());
		x.put("service_type", s.getServiceType());
		x.put("duration_minutes", s.getDurationMinutes());
		x.put("description", s.getDescription());
		x.put("base_price", s.getBasePrice());
		x.put("zone_name", s.getZone() == null ? null : s.getZone().getZoneName());
		return x;
	}

	private Map<String, Object> trainerCard(Trainer t) {
		Map<String, Object> x = new LinkedHashMap<>();
		String fn = t.getPersonalData() == null ? "" : (t.getPersonalData().getLastName() + " " + t.getPersonalData().getFirstName()).trim();
		x.put("trainer_id", t.getTrainerId());
		x.put("full_name", fn);
		x.put("specialization", t.getSpecialization());
		x.put("experience_years", t.getExperienceYears());
		x.put("rating", t.getRating());
		x.put("biography", t.getBiography());
		return x;
	}

	private Map<String, Object> trainerMini(Trainer t) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("trainer_id", t.getTrainerId());
		x.put("full_name", t.getPersonalData().getLastName() + " " + t.getPersonalData().getFirstName());
		return x;
	}

	private Map<String, Object> zoneRow(Zone z) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("zone_id", z.getZoneId());
		x.put("zone_name", z.getZoneName());
		return x;
	}

	private Map<String, Object> scheduleRow(ScheduleSlot s) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("schedule_id", s.getScheduleId());
		x.put("service_id", s.getService().getServiceId());
		x.put("service_name", s.getService().getServiceName());
		x.put("service_type", s.getService().getServiceType());
		x.put("trainer_id", s.getTrainer().getTrainerId());
		x.put("trainer_name", s.getTrainer().getPersonalData().getLastName() + " " + s.getTrainer().getPersonalData().getFirstName());
		x.put("hall_id", s.getHall().getHallId());
		x.put("hall_name", s.getHall().getHallName());
		x.put("zone_id", s.getHall().getZone() == null ? null : s.getHall().getZone().getZoneId());
		x.put("zone_name", s.getHall().getZone() == null ? null : s.getHall().getZone().getZoneName());
		x.put("start_datetime", s.getStartDatetime());
		x.put("end_datetime", s.getEndDatetime());
		x.put("max_participants", s.getMaxParticipants());
		x.put("current_participants", s.getCurrentParticipants());
		return x;
	}

	private Map<String, Object> documentRow(DocumentEntity d) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("document_id", d.getDocumentId());
		x.put("document_type", d.getDocumentType().name());
		x.put("series", d.getSeries());
		x.put("number", d.getNumber());
		x.put("issued_by", d.getIssuedBy());
		x.put("issue_date", d.getIssueDate());
		x.put("expiry_date", d.getExpiryDate());
		x.put("notes", d.getNotes());
		x.put("is_verified", Boolean.TRUE.equals(d.getVerified()));
		return x;
	}

	private Map<String, Object> notificationRow(InAppNotification n) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("sent_datetime", n.getSentDatetime());
		x.put("notification_type", n.getNotificationType() == null ? "system" : n.getNotificationType());
		if (n.getSenderTrainer() != null && n.getSenderTrainer().getPersonalData() != null) {
			x.put("sender", fullName(n.getSenderTrainer().getPersonalData()));
			x.put("sender_role", "Тренер");
		} else {
			x.put("sender", "Администрация клуба");
			x.put("sender_role", "Администратор");
		}
		x.put("title", n.getTitle());
		x.put("message", n.getMessage());
		return x;
	}

	private Map<String, Object> membershipPurchaseRow(MembershipPurchase p) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("membership_name", p.getMembership().getMembershipName());
		x.put("remaining_freeze_days", p.getMembership().getFreezeDays());
		x.put("remaining_guest_visits", p.getRemainingGuestVisits());
		x.put("start_date", p.getStartDate());
		x.put("end_date", p.getEndDate());
		x.put("status", p.getStatus());
		return x;
	}

	private List<Map<String, Object>> membershipRulesForPurchase(MembershipPurchase p) {
		List<MembershipServiceLink> links = membershipServiceLinkRepository.findByMembershipIdOrderByServiceIdAsc(p.getMembership().getMembershipId());
		return links.stream().map(ms -> {
			FitnessServiceEntity s = serviceRepository.findById(ms.getServiceId()).orElse(null);
			Long used = membershipUsageRecordRepository.countByMembershipPurchase_PurchaseIdAndService_ServiceId(p.getPurchaseId(), ms.getServiceId());
			Integer included = ms.getIncludedVisits();
			Integer rem = included == null ? null : Math.max(0, included - used.intValue());
			Map<String, Object> x = new LinkedHashMap<>();
			x.put("service_name", s == null ? "—" : s.getServiceName());
			x.put("included_visits", included == null ? "безлимит" : included);
			x.put("remaining", rem == null ? "—" : rem);
			x.put("max_per_week", ms.getMaxPerWeek());
			x.put("days_of_week", ms.getDaysOfWeek());
			x.put("time_start", ms.getTimeStart());
			x.put("time_end", ms.getTimeEnd());
			return x;
		}).collect(Collectors.toList());
	}

	private Map<String, Object> servicePurchaseRuleRow(ServicePurchaseEntity p) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("purchase_id", p.getPurchaseId());
		x.put("purchase_date", purchaseDateDisplayString(p.getPurchaseDate()));
		x.put("service_name", p.getService().getServiceName());
		x.put("included_visits", 1);
		x.put("remaining", 1);
		x.put("max_per_week", "—");
		if (p.getSchedule() != null && p.getSchedule().getStartDatetime() != null) {
			var d = p.getSchedule().getStartDatetime();
			x.put("days_of_week", d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("ru")));
			x.put("time_start", d.toLocalTime());
			x.put("time_end", p.getSchedule().getEndDatetime() == null ? null : p.getSchedule().getEndDatetime().toLocalTime());
		} else {
			x.put("days_of_week", "—");
			x.put("time_start", "—");
			x.put("time_end", "—");
		}
		return x;
	}

	private Map<String, Object> personalRegistrationRow(ServicePurchaseEntity p) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("purchase_id", p.getPurchaseId());
		x.put("schedule_id", p.getSchedule() == null ? null : p.getSchedule().getScheduleId());
		x.put("trainer_name", p.getSchedule() == null ? "—" : p.getSchedule().getTrainer().getPersonalData().getLastName() + " " + p.getSchedule().getTrainer().getPersonalData().getFirstName());
		x.put("hall_name", p.getSchedule() == null ? "—" : p.getSchedule().getHall().getHallName());
		x.put("zone_name", p.getSchedule() == null || p.getSchedule().getHall().getZone() == null ? "—" : p.getSchedule().getHall().getZone().getZoneName());
		x.put("start_datetime", p.getSchedule() == null ? null : p.getSchedule().getStartDatetime());
		x.put("purchase_date", purchaseDateDisplayString(p.getPurchaseDate()));
		return x;
	}

	private static String purchaseDateDisplayString(LocalDateTime t) {
		return t == null ? null : PURCHASE_DATE_DISPLAY.format(t);
	}

	private static String fullName(PersonalData p) {
		if (p == null) return "—";
		String mn = p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : " " + p.getMiddleName();
		return (p.getLastName() + " " + p.getFirstName() + mn).trim();
	}

	private static String str(Map<String, Object> body, String key) {
		Object v = body.get(key);
		return v == null ? null : String.valueOf(v).trim();
	}

	private Set<Integer> availablePersonalTrainingZoneIds(Client client) {
		Set<Integer> zoneIds = new LinkedHashSet<>();
		Integer clientId = client.getClientId();

		List<MembershipPurchase> activeMemberships = membershipPurchaseRepository.findActiveForClientOnDate(
				clientId, LocalDate.now(), "активен");
		for (MembershipPurchase purchase : activeMemberships) {
			Integer membershipId = purchase.getMembership() == null ? null : purchase.getMembership().getMembershipId();
			if (membershipId == null) continue;
			for (MembershipServiceLink link : membershipServiceLinkRepository.findByMembershipIdOrderByServiceIdAsc(membershipId)) {
				FitnessServiceEntity service = serviceRepository.findById(link.getServiceId()).orElse(null);
				if (service == null || !isPersonalType(service.getServiceType()) || service.getZone() == null) continue;
				zoneIds.add(service.getZone().getZoneId());
			}
		}

		for (ServicePurchaseEntity purchase : servicePurchaseRepository.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(clientId)) {
			FitnessServiceEntity service = purchase.getService();
			if (service == null || !isPersonalType(service.getServiceType()) || service.getZone() == null) continue;
			zoneIds.add(service.getZone().getZoneId());
		}

		return zoneIds;
	}
}

