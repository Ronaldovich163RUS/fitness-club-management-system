package com.fitnessclub.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.ClubZone;
import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.AvailableServiceRowDto;
import com.fitnessclub.api.dto.ClubStatsResponseDto;
import com.fitnessclub.api.dto.OpenVisitClientDto;
import com.fitnessclub.api.dto.UseServiceRequest;
import com.fitnessclub.api.dto.VisitEntryRequest;
import com.fitnessclub.api.dto.VisitExitRequest;
import com.fitnessclub.api.dto.VisitJournalRowDto;
import com.fitnessclub.domain.ClubStats;
import com.fitnessclub.domain.ClubVisit;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.MembershipPurchase;
import com.fitnessclub.domain.MembershipServiceLink;
import com.fitnessclub.domain.MembershipUsageRecord;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.ScheduleSlot;
import com.fitnessclub.domain.ServicePurchaseEntity;
import com.fitnessclub.repository.ClubStatsRepository;
import com.fitnessclub.repository.ClubVisitRepository;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.MembershipPurchaseRepository;
import com.fitnessclub.repository.MembershipServiceLinkRepository;
import com.fitnessclub.repository.MembershipUsageRecordRepository;
import com.fitnessclub.repository.ScheduleSlotRepository;
import com.fitnessclub.repository.ServicePurchaseEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminVisitService {

	private static final String ACTIVE_STATUS = "активен";
	private static final String VISIT_REGULAR = "обычный";
	private static final String VISIT_GUEST = "гостевой";
	private static final String GROUP_TYPE = "групповая тренировка";
	private static final String PERSONAL_TYPE = "персональная тренировка";
	private static final String SOURCE_MEMBERSHIP = "абонемент";
	private static final String SOURCE_PURCHASE = "разовая";
	/** Групповая и персональная: занятие начинается не позже чем через столько минут (или уже идёт). */
	private static final int MAX_MINUTES_UNTIL_GROUP_START = 30;
	private static final int MAX_MINUTES_UNTIL_PERSONAL_START = 30;

	private final ClubVisitRepository clubVisitRepository;
	private final ClubStatsRepository clubStatsRepository;
	private final ClientRepository clientRepository;
	private final MembershipPurchaseRepository membershipPurchaseRepository;
	private final MembershipUsageRecordRepository membershipUsageRecordRepository;
	private final MembershipServiceLinkRepository membershipServiceLinkRepository;
	private final ServicePurchaseEntityRepository servicePurchaseEntityRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final FitnessServiceRepository fitnessServiceRepository;
	private final CurrentAdminIdResolver adminIdResolver;

	private ClubStats statsRow() {
		return clubStatsRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
			ClubStats s = new ClubStats();
			s.setCurrentVisitors(0);
			return clubStatsRepository.save(s);
		});
	}

	@Transactional(readOnly = true)
	public ClubStatsResponseDto clubStats() {
		ClubStats s = statsRow();
		return ClubStatsResponseDto.builder()
				.currentVisitors(s.getCurrentVisitors() != null ? s.getCurrentVisitors() : 0)
				.build();
	}

	@Transactional(readOnly = true)
	public List<VisitJournalRowDto> journal() {
		return clubVisitRepository.findAllFetchClientOrderByEntryDesc().stream()
				.map(this::toJournalRow)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<OpenVisitClientDto> openVisitClients() {
		return clubVisitRepository.findOpenVisitsFetchClient().stream()
				.map(v -> OpenVisitClientDto.builder()
						.clientId(v.getClient().getClientId())
						.clientFullName(fullName(v.getClient().getPersonalData()))
						.build())
				.distinct()
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AvailableServiceRowDto> availableServices(Integer clientId) {
		clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		List<AvailableServiceRowDto> out = new ArrayList<>();
		Set<Integer> fixedOnVisit = serviceIdsFixedOnCurrentVisit(clientId);
		List<MembershipPurchase> activePurchases = membershipPurchaseRepository.findActiveForClientOnDate(clientId,
				LocalDate.now(), ACTIVE_STATUS);
		if (!activePurchases.isEmpty()) {
			MembershipPurchase p = activePurchases.get(0);
			Integer mid = p.getMembership().getMembershipId();
			LocalDateTime now = now();
			for (MembershipServiceLink ms : membershipServiceLinkRepository.findByMembershipIdOrderByServiceIdAsc(mid)) {
				if (!passesDaysAndTime(ms, now)) {
					continue;
				}
				FitnessServiceEntity svc = fitnessServiceRepository.findById(ms.getServiceId()).orElse(null);
				if (svc == null || !isServiceActive(svc)) {
					continue;
				}
				if (fixedOnVisit.contains(svc.getServiceId())) {
					continue;
				}
				long used = membershipUsageRecordRepository.countByMembershipPurchase_PurchaseIdAndService_ServiceId(
						p.getPurchaseId(), ms.getServiceId());
				Integer inc = ms.getIncludedVisits();
				if (inc != null && used >= inc) {
					continue;
				}
				Integer maxWeek = ms.getMaxPerWeek();
				if (maxWeek != null) {
					LocalDateTime since = now.minusDays(7);
					long weekCnt = membershipUsageRecordRepository.countForPurchaseServiceSince(p.getPurchaseId(),
							ms.getServiceId(), since);
					if (weekCnt >= maxWeek) {
						continue;
					}
				}
				String details = inc == null ? "безлимит" : "осталось " + (inc - (int) used);
				String stype = normalizeServiceType(svc.getServiceType());
				if (GROUP_TYPE.equals(stype)) {
					List<ScheduleSlot> slots = findGroupSlotsForService(svc.getServiceId(), now,
							MAX_MINUTES_UNTIL_GROUP_START);
					if (slots.isEmpty()) {
						out.add(availableMembershipRow(p, svc, null,
								details + " (нет группового занятия в ближайшие " + MAX_MINUTES_UNTIL_GROUP_START
										+ " мин)",
								null, false));
					} else {
						for (ScheduleSlot slot : slots) {
							String slotDetails = details + ", " + formatSlotHint(slot);
							boolean hasCapacity = isGroupSlotHasCapacity(slot);
							if (!hasCapacity) {
								slotDetails += " (нет свободных мест)";
							}
							out.add(availableMembershipRow(p, svc, slot, slotDetails, null, hasCapacity));
						}
					}
				} else if (PERSONAL_TYPE.equals(stype)) {
					List<ServicePurchaseEntity> bookings = findPersonalBookingsInWindow(clientId, svc.getServiceId(), now);
					if (bookings.isEmpty()) {
						out.add(availableMembershipRow(p, svc, null,
								details + " (нет записи на персональное занятие в ближайшие "
										+ MAX_MINUTES_UNTIL_PERSONAL_START + " мин)",
								null, false));
					} else {
						for (ServicePurchaseEntity booking : bookings) {
							ScheduleSlot slot = booking.getSchedule();
							out.add(availableMembershipRow(p, svc, slot,
									details + ", " + formatSlotHint(slot), booking.getPurchaseId(), true));
						}
					}
				} else {
					out.add(availableMembershipRow(p, svc, null, details, null, true));
				}
			}
		}
		for (ServicePurchaseEntity sp : servicePurchaseEntityRepository
				.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(clientId)) {
			FitnessServiceEntity svc = sp.getService();
			if (svc == null || !isServiceActive(svc)) {
				continue;
			}
			if (fixedOnVisit.contains(svc.getServiceId())) {
				continue;
			}
			LocalDateTime now = now();
			ScheduleSlot sched = sp.getSchedule();
			String stype = normalizeServiceType(svc.getServiceType());
			if (GROUP_TYPE.equals(stype)) {
				List<ScheduleSlot> slots = findGroupSlotsForService(svc.getServiceId(), now,
						MAX_MINUTES_UNTIL_GROUP_START);
				if (slots.isEmpty()) {
					out.add(availablePurchaseRow(sp, svc, null,
							"нет группового занятия в ближайшие " + MAX_MINUTES_UNTIL_GROUP_START + " мин", false));
				} else {
					for (ScheduleSlot slot : slots) {
						String slotDetails = formatSlotHint(slot);
						boolean hasCapacity = isGroupSlotHasCapacity(slot);
						if (!hasCapacity) {
							slotDetails += " (нет свободных мест)";
						}
						out.add(availablePurchaseRow(sp, svc, slot, slotDetails, hasCapacity));
					}
				}
				continue;
			}
			if (PERSONAL_TYPE.equals(stype)) {
				if (sched == null || !isPersonalSlotFixable(sched, now)) {
					String hint = sched == null ? "нет записи на занятие"
							: formatSlotHint(sched) + " (занятие не в ближайшие " + MAX_MINUTES_UNTIL_PERSONAL_START
									+ " мин)";
					out.add(availablePurchaseRow(sp, svc, null, hint, false));
				} else {
					out.add(availablePurchaseRow(sp, svc, sched, formatSlotHint(sched), true));
				}
				continue;
			}
			String details = sched == null ? "без привязки" : formatSlotHint(sched);
			out.add(availablePurchaseRow(sp, svc, sched, details, true));
		}
		return out;
	}

	@Transactional
	public void registerEntry(VisitEntryRequest req) {
		MapErr err = new MapErr();
		Integer clientId = req.getClientId();
		if (clientId == null) {
			err.add("client_id", "Укажите клиента.");
		}
		String vtype = req.getVisitType() == null ? "" : req.getVisitType().trim().toLowerCase(Locale.ROOT);
		if (!VISIT_REGULAR.equals(vtype) && !VISIT_GUEST.equals(vtype)) {
			err.add("visit_type", "Тип визита: «обычный» или «гостевой».");
		}
		err.throwIfAny();
		Client client = clientRepository.findByIdFetchPersonal(clientId)
				.orElseThrow(() -> ApiValidationException.single("client_id", "Клиент не найден."));
		if (clubVisitRepository.existsByClient_ClientIdAndExitDatetimeIsNull(clientId)) {
			throw ApiValidationException.single("client_id", "У клиента уже есть открытое посещение.");
		}
		List<MembershipPurchase> activeOnEntry = membershipPurchaseRepository.findActiveForClientOnDate(clientId,
				LocalDate.now(), ACTIVE_STATUS);
		if (activeOnEntry.isEmpty()) {
			throw ApiValidationException.single("client_id", "Нет активного абонемента на сегодня.");
		}
		MembershipPurchase purchase = activeOnEntry.get(0);
		if (purchase.getFreezeStartedOn() != null && !hasAvailableOneTimePurchase(clientId)) {
			throw ApiValidationException.single("client_id",
					"Ошибка! Клиент не может пользоваться услугами клуба, так как его абоенмент заморожен и не имеется ни одной доступной разовой услуги.");
		}
		if (VISIT_GUEST.equals(vtype)) {
			Integer left = purchase.getRemainingGuestVisits();
			if (left == null || left <= 0) {
				throw ApiValidationException.single("visit_type", "Нет доступных гостевых визитов.");
			}
			purchase.setRemainingGuestVisits(left - 1);
		}
		ClubVisit v = new ClubVisit();
		v.setClient(client);
		v.setEntryDatetime(now());
		v.setVisitType(VISIT_GUEST.equals(vtype) ? VISIT_GUEST : VISIT_REGULAR);
		v.setRegisteredByAdminId(adminIdResolver.requireDefaultAdminId());
		v.setServiceFixed(Boolean.FALSE);
		clubVisitRepository.save(v);
		ClubStats st = statsRow();
		int delta = VISIT_GUEST.equals(vtype) ? 2 : 1;
		st.setCurrentVisitors((st.getCurrentVisitors() == null ? 0 : st.getCurrentVisitors()) + delta);
		clubStatsRepository.save(st);
	}

	@Transactional
	public void registerExit(VisitExitRequest req) {
		Integer clientId = req.getClientId();
		if (clientId == null) {
			throw ApiValidationException.single("client_id", "Укажите клиента.");
		}
		List<ClubVisit> open = clubVisitRepository.findByClient_ClientIdAndExitDatetimeIsNullOrderByEntryDatetimeAsc(clientId);
		if (open.isEmpty()) {
			throw ApiValidationException.single("client_id", "Нет открытого визита для этого клиента.");
		}
		ClubVisit last = open.get(0);
		last.setExitDatetime(now());
		clubVisitRepository.save(last);
		ClubStats st = statsRow();
		int delta = last.getVisitType() != null && last.getVisitType().equalsIgnoreCase(VISIT_GUEST) ? 2 : 1;
		st.setCurrentVisitors(Math.max(0, (st.getCurrentVisitors() == null ? 0 : st.getCurrentVisitors()) - delta));
		clubStatsRepository.save(st);
	}

	@Transactional
	public void useService(UseServiceRequest req) {
		String source = req.getSource() == null ? "" : req.getSource().trim().toLowerCase(Locale.ROOT);
		Integer clientId = req.getClientId();
		Integer serviceId = req.getServiceId();
		MapErr err = new MapErr();
		if (clientId == null) {
			err.add("client_id", "Укажите клиента.");
		}
		if (serviceId == null) {
			err.add("service_id", "Укажите услугу.");
		}
		err.throwIfAny();
		if (!clubVisitRepository.existsByClient_ClientIdAndExitDatetimeIsNull(clientId)) {
			throw ApiValidationException.single("client_id", "Клиент должен быть в клубе (открытый визит).");
		}
		ensureServiceNotAlreadyFixedOnCurrentVisit(clientId, serviceId);
		if (SOURCE_MEMBERSHIP.equals(source)) {
			Integer pid = req.getMembershipPurchaseId();
			if (pid == null) {
				throw ApiValidationException.single("membership_purchase_id", "Укажите покупку абонемента.");
			}
			MembershipPurchase p = membershipPurchaseRepository.findById(pid)
					.orElseThrow(() -> ApiValidationException.single("membership_purchase_id", "Покупка не найдена."));
			if (!p.getClient().getClientId().equals(clientId)) {
				throw ApiValidationException.single("membership_purchase_id", "Покупка принадлежит другому клиенту.");
			}
			membershipServiceLinkRepository
					.findByMembershipIdAndServiceId(p.getMembership().getMembershipId(), serviceId)
					.orElseThrow(() -> ApiValidationException.single("service_id", "Услуга не входит в абонемент."));
			FitnessServiceEntity svc = fitnessServiceRepository.findById(serviceId).orElseThrow();
			LocalDateTime now = now();
			String stype = normalizeServiceType(svc.getServiceType());
			ScheduleSlot linkedSlot = null;
			if (GROUP_TYPE.equals(stype)) {
				ScheduleSlot sch = resolveGroupScheduleForUse(req.getScheduleId(), serviceId, now,
						MAX_MINUTES_UNTIL_GROUP_START);
				validateGroupSlotForUse(sch, now, MAX_MINUTES_UNTIL_GROUP_START);
				ensureTrainingFixationAllowed(clientId, sch);
				incrementGroupParticipants(sch);
				linkedSlot = sch;
			} else if (PERSONAL_TYPE.equals(stype)) {
				ServicePurchaseEntity booking = resolvePersonalBookingForUse(clientId, serviceId, req.getScheduleId(),
						now);
				ScheduleSlot sch = scheduleSlotRepository.findByIdFetchRelations(booking.getSchedule().getScheduleId())
						.orElseThrow(() -> ApiValidationException.single("schedule_id", "Занятие не найдено."));
				ensureTrainerActiveForSlot(sch);
				validatePersonalBookingForUse(booking, clientId, sch, now);
				ensureTrainingFixationAllowed(clientId, sch);
				markServicePurchaseUsed(booking, now);
				linkedSlot = sch;
			}
			MembershipUsageRecord u = new MembershipUsageRecord();
			u.setMembershipPurchase(p);
			u.setService(svc);
			u.setUsedAt(now);
			membershipUsageRecordRepository.save(u);
			if (linkedSlot != null) {
				attachScheduleToOpenVisit(clientId, linkedSlot);
			}
			return;
		}
		if (SOURCE_PURCHASE.equals(source)) {
			Integer spid = req.getServicePurchaseId();
			if (spid == null) {
				throw ApiValidationException.single("service_purchase_id", "Укажите разовую покупку.");
			}
			ServicePurchaseEntity sp = servicePurchaseEntityRepository.findByIdWithDetails(spid)
					.orElseThrow(() -> ApiValidationException.single("service_purchase_id", "Покупка не найдена."));
			if (!sp.getClient().getClientId().equals(clientId)) {
				throw ApiValidationException.single("service_purchase_id", "Покупка принадлежит другому клиенту.");
			}
			if (Boolean.TRUE.equals(sp.getIsUsed())) {
				throw ApiValidationException.single("service_purchase_id", "Услуга уже использована.");
			}
			if (!sp.getService().getServiceId().equals(serviceId)) {
				throw ApiValidationException.single("service_id", "Несовпадение услуги.");
			}
			Integer reqSched = req.getScheduleId();
			LocalDateTime now = now();
			String stype = normalizeServiceType(sp.getService().getServiceType());
			ScheduleSlot linkedSlot = null;
			if (GROUP_TYPE.equals(stype)) {
				ScheduleSlot sch = resolveGroupScheduleForUse(reqSched, serviceId, now,
						MAX_MINUTES_UNTIL_GROUP_START);
				validateGroupSlotForUse(sch, now, MAX_MINUTES_UNTIL_GROUP_START);
				ensureTrainingFixationAllowed(clientId, sch);
				incrementGroupParticipants(sch);
				linkedSlot = sch;
			} else if (PERSONAL_TYPE.equals(stype)) {
				ScheduleSlot purchaseSlot = sp.getSchedule();
				if (purchaseSlot == null) {
					throw ApiValidationException.single("schedule_id",
							"Для персональной тренировки требуется запись на занятие.");
				}
				Integer purchaseSchedId = purchaseSlot.getScheduleId();
				if (reqSched != null && !Objects.equals(reqSched, purchaseSchedId)) {
					throw ApiValidationException.single("schedule_id", "Укажите корректное занятие по расписанию.");
				}
				ScheduleSlot sch = scheduleSlotRepository.findByIdFetchRelations(purchaseSchedId)
						.orElseThrow(() -> ApiValidationException.single("schedule_id", "Занятие не найдено."));
				ensureTrainerActiveForSlot(sch);
				validatePersonalBookingForUse(sp, clientId, sch, now);
				ensureTrainingFixationAllowed(clientId, sch);
				linkedSlot = sch;
			}
			markServicePurchaseUsed(sp, now);
			if (linkedSlot != null) {
				attachScheduleToOpenVisit(clientId, linkedSlot);
			}
			return;
		}
		throw ApiValidationException.single("source", "Источник: «абонемент» или «разовая».");
	}

	private VisitJournalRowDto toJournalRow(ClubVisit v) {
		return VisitJournalRowDto.builder()
				.visitId(v.getVisitId())
				.clientFullName(fullName(v.getClient().getPersonalData()))
				.entryDatetime(v.getEntryDatetime())
				.exitDatetime(v.getExitDatetime())
				.visitType(v.getVisitType())
				.build();
	}

	private String fullName(PersonalData p) {
		String mn = p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : " " + p.getMiddleName();
		return (p.getLastName() + " " + p.getFirstName() + mn).trim();
	}

	private boolean passesDaysAndTime(MembershipServiceLink ms, LocalDateTime now) {
		String days = ms.getDaysOfWeek();
		if (days != null && !days.isBlank()) {
			Set<String> allowed = new LinkedHashSet<>();
			for (String p : days.split(",")) {
				if (p != null && !p.isBlank()) {
					allowed.add(p.trim().toLowerCase(Locale.ROOT));
				}
			}
			if (!allowed.isEmpty() && !allowed.contains(ruDay(now.toLocalDate()))) {
				return false;
			}
		}
		LocalTime t = now.toLocalTime();
		LocalTime ts = ms.getTimeStart();
		LocalTime te = ms.getTimeEnd();
		if (ts != null && t.isBefore(ts)) {
			return false;
		}
		if (te != null) {
			if (LocalTime.MIDNIGHT.equals(te)) {
				if (ts != null && (t.isBefore(ts) || t.equals(LocalTime.MIDNIGHT))) {
					return false;
				}
			} else if (t.isAfter(te)) {
				return false;
			}
		}
		return true;
	}

	private boolean hasAvailableOneTimePurchase(Integer clientId) {
		LocalDateTime now = now();
		for (ServicePurchaseEntity sp : servicePurchaseEntityRepository
				.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(clientId)) {
			FitnessServiceEntity svc = sp.getService();
			if (svc == null || !Boolean.TRUE.equals(svc.getIsActive())) {
				continue;
			}
			ScheduleSlot sched = sp.getSchedule();
			if (sched == null) {
				return true;
			}
			String stype = normalizeServiceType(svc.getServiceType());
			if (GROUP_TYPE.equals(stype)) {
				if (findGroupSlotsForService(svc.getServiceId(), now, MAX_MINUTES_UNTIL_GROUP_START).stream()
						.anyMatch(this::isGroupSlotHasCapacity)) {
					return true;
				}
				continue;
			}
			if (PERSONAL_TYPE.equals(stype)) {
				if (sched == null || !isPersonalSlotFixable(sched, now) || !trainerSlotIsConductable(sched)) {
					continue;
				}
			}
			return true;
		}
		return false;
	}

	private LocalDateTime now() {
		return LocalDateTime.now(ClubZone.ZONE);
	}

	private static LocalDateTime latestGroupStart(LocalDateTime now, int maxMinutesUntilStart) {
		return now.plusMinutes(maxMinutesUntilStart);
	}

	private static String normalizeServiceType(String raw) {
		return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
	}

	private List<ScheduleSlot> findGroupSlotsForService(Integer serviceId, LocalDateTime now,
			int maxMinutesUntilStart) {
		return scheduleSlotRepository.findSlotsForServiceInUseWindow(serviceId, now,
				latestGroupStart(now, maxMinutesUntilStart));
	}

	private boolean isGroupSlotFixable(ScheduleSlot sched, LocalDateTime now, int maxMinutesUntilStart) {
		if (sched == null || Boolean.TRUE.equals(sched.getIsCancelled())) {
			return false;
		}
		LocalDateTime start = sched.getStartDatetime();
		LocalDateTime end = sched.getEndDatetime();
		if (start == null || end == null) {
			return false;
		}
		return !now.isAfter(end) && !start.isAfter(latestGroupStart(now, maxMinutesUntilStart));
	}

	private boolean isPersonalSlotFixable(ScheduleSlot sched, LocalDateTime now) {
		return isGroupSlotFixable(sched, now, MAX_MINUTES_UNTIL_PERSONAL_START);
	}

	private List<ServicePurchaseEntity> findPersonalBookingsInWindow(Integer clientId, Integer serviceId,
			LocalDateTime now) {
		return servicePurchaseEntityRepository.findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(clientId).stream()
				.filter(sp -> sp.getService() != null && serviceId.equals(sp.getService().getServiceId()))
				.filter(sp -> PERSONAL_TYPE.equals(normalizeServiceType(sp.getService().getServiceType())))
				.filter(sp -> sp.getSchedule() != null && isPersonalSlotFixable(sp.getSchedule(), now))
				.collect(Collectors.toList());
	}

	private boolean isGroupSlotHasCapacity(ScheduleSlot sched) {
		int cur = sched.getCurrentParticipants() == null ? 0 : sched.getCurrentParticipants();
		int max = sched.getMaxParticipants() == null ? 0 : sched.getMaxParticipants();
		return max > 0 && cur < max;
	}

	private LocalDateTime openVisitEntrySince(Integer clientId) {
		List<ClubVisit> open = clubVisitRepository.findByClient_ClientIdAndExitDatetimeIsNullOrderByEntryDatetimeAsc(
				clientId);
		if (open.isEmpty()) {
			return null;
		}
		return open.get(0).getEntryDatetime();
	}

	private Set<Integer> serviceIdsFixedOnCurrentVisit(Integer clientId) {
		LocalDateTime since = openVisitEntrySince(clientId);
		if (since == null) {
			return Set.of();
		}
		Set<Integer> ids = new HashSet<>(
				membershipUsageRecordRepository.findDistinctServiceIdsForClientSince(clientId, since));
		ids.addAll(servicePurchaseEntityRepository.findDistinctServiceIdsUsedByClientSince(clientId, since));
		return ids;
	}

	private void ensureServiceNotAlreadyFixedOnCurrentVisit(Integer clientId, Integer serviceId) {
		LocalDateTime since = openVisitEntrySince(clientId);
		if (since == null) {
			return;
		}
		if (membershipUsageRecordRepository.existsForClientServiceSince(clientId, serviceId, since)
				|| servicePurchaseEntityRepository.existsUsedForClientServiceSince(clientId, serviceId, since)) {
			throw ApiValidationException.single("service_id", "Эта услуга уже зафиксирована на текущий визит.");
		}
	}

	private void markServicePurchaseUsed(ServicePurchaseEntity purchase, LocalDateTime usedAt) {
		purchase.setIsUsed(Boolean.TRUE);
		purchase.setUsedAt(usedAt);
		servicePurchaseEntityRepository.save(purchase);
	}

	private void ensureClientNotAlreadyFixedForSchedule(Integer clientId, Integer scheduleId) {
		if (scheduleId == null) {
			return;
		}
		if (clubVisitRepository.existsByClient_ClientIdAndSchedule_ScheduleId(clientId, scheduleId)
				|| servicePurchaseEntityRepository.existsUsedByClient_ClientIdAndSchedule_ScheduleId(clientId,
						scheduleId)) {
			throw ApiValidationException.single("schedule_id", "Посещение этого занятия уже было зафиксировано.");
		}
	}

	private void ensureTrainingFixationAllowed(Integer clientId, ScheduleSlot sch) {
		if (sch != null) {
			ensureClientNotAlreadyFixedForSchedule(clientId, sch.getScheduleId());
		}
	}

	private void validateGroupSlotForUse(ScheduleSlot sch, LocalDateTime now, int maxMinutesUntilStart) {
		if (!isGroupSlotFixable(sch, now, maxMinutesUntilStart)) {
			throw ApiValidationException.single("schedule_id",
					"Групповую тренировку можно зафиксировать, если занятие начинается не позже чем через "
							+ maxMinutesUntilStart + " мин или уже идёт.");
		}
		ensureTrainerActiveForSlot(sch);
		int cur = sch.getCurrentParticipants() == null ? 0 : sch.getCurrentParticipants();
		int max = sch.getMaxParticipants() == null ? 0 : sch.getMaxParticipants();
		if (max <= 0 || cur >= max) {
			throw ApiValidationException.single("schedule_id",
					"Посещаемость занятия уже заполнена, зафиксировать услугу нельзя.");
		}
	}

	private void validatePersonalBookingForUse(ServicePurchaseEntity sp, Integer clientId, ScheduleSlot sch,
			LocalDateTime now) {
		if (!sp.getClient().getClientId().equals(clientId)) {
			throw ApiValidationException.single("service_purchase_id", "Покупка принадлежит другому клиенту.");
		}
		if (!PERSONAL_TYPE.equals(normalizeServiceType(sp.getService().getServiceType()))) {
			throw ApiValidationException.single("service_id", "Услуга не является персональной тренировкой.");
		}
		if (Boolean.TRUE.equals(sp.getIsUsed())) {
			throw ApiValidationException.single("service_purchase_id", "Услуга уже использована.");
		}
		if (sp.getSchedule() == null || !Objects.equals(sp.getSchedule().getScheduleId(), sch.getScheduleId())) {
			throw ApiValidationException.single("schedule_id", "У клиента нет записи на это занятие.");
		}
		if (!isPersonalSlotFixable(sch, now)) {
			throw ApiValidationException.single("schedule_id",
					"Персональную тренировку можно зафиксировать, если занятие начинается не позже чем через "
							+ MAX_MINUTES_UNTIL_PERSONAL_START + " мин или уже идёт.");
		}
	}

	private ServicePurchaseEntity resolvePersonalBookingForUse(Integer clientId, Integer serviceId, Integer scheduleId,
			LocalDateTime now) {
		if (scheduleId != null) {
			ServicePurchaseEntity sp = servicePurchaseEntityRepository
					.findFirstByClient_ClientIdAndSchedule_ScheduleIdAndIsUsedFalse(clientId, scheduleId)
					.orElseThrow(() -> ApiValidationException.single("schedule_id", "У клиента нет записи на это занятие."));
			if (!sp.getService().getServiceId().equals(serviceId)) {
				throw ApiValidationException.single("schedule_id", "Занятие не соответствует выбранной услуге.");
			}
			if (!isPersonalSlotFixable(sp.getSchedule(), now)) {
				throw ApiValidationException.single("schedule_id",
						"Персональную тренировку можно зафиксировать, если занятие начинается не позже чем через "
								+ MAX_MINUTES_UNTIL_PERSONAL_START + " мин или уже идёт.");
			}
			return sp;
		}
		return findPersonalBookingsInWindow(clientId, serviceId, now).stream()
				.findFirst()
				.orElseThrow(() -> ApiValidationException.single("schedule_id",
						"Нет записи на персональное занятие в ближайшие " + MAX_MINUTES_UNTIL_PERSONAL_START
								+ " мин. Укажите занятие вручную."));
	}

	private ScheduleSlot resolveGroupScheduleForUse(Integer scheduleId, Integer serviceId, LocalDateTime now,
			int maxMinutesUntilStart) {
		if (scheduleId != null) {
			ScheduleSlot sch = scheduleSlotRepository.findByIdFetchRelations(scheduleId)
					.orElseThrow(() -> ApiValidationException.single("schedule_id", "Занятие не найдено."));
			if (!sch.getService().getServiceId().equals(serviceId)) {
				throw ApiValidationException.single("schedule_id", "Занятие не соответствует выбранной услуге.");
			}
			return sch;
		}
		List<ScheduleSlot> slots = findGroupSlotsForService(serviceId, now, maxMinutesUntilStart);
		return slots.stream()
				.filter(this::isGroupSlotHasCapacity)
				.findFirst()
				.orElseThrow(() -> ApiValidationException.single("schedule_id",
						"Нет группового занятия в ближайшие " + maxMinutesUntilStart
								+ " мин со свободными местами."));
	}

	private void ensureTrainerActiveForSlot(ScheduleSlot sch) {
		if (!trainerSlotIsConductable(sch)) {
			throw ApiValidationException.single("schedule_id", "Услуга с этим занятием недоступна: тренер не активен.");
		}
	}

	private boolean isServiceActive(FitnessServiceEntity svc) {
		return svc != null && !Boolean.FALSE.equals(svc.getIsActive());
	}

	private boolean trainerSlotIsConductable(ScheduleSlot sch) {
		return sch.getTrainer() != null && !Boolean.FALSE.equals(sch.getTrainer().getIsActive());
	}

	private void attachScheduleToOpenVisit(Integer clientId, ScheduleSlot sch) {
		List<ClubVisit> open = clubVisitRepository.findByClient_ClientIdAndExitDatetimeIsNullOrderByEntryDatetimeAsc(
				clientId);
		if (open.isEmpty()) {
			return;
		}
		ClubVisit visit = open.get(0);
		if (visit.getSchedule() == null) {
			visit.setSchedule(sch);
			clubVisitRepository.save(visit);
		}
	}

	private void incrementGroupParticipants(ScheduleSlot sch) {
		ScheduleSlot fresh = scheduleSlotRepository.findById(sch.getScheduleId()).orElse(sch);
		int cur = fresh.getCurrentParticipants() == null ? 0 : fresh.getCurrentParticipants();
		int max = fresh.getMaxParticipants() == null ? 0 : fresh.getMaxParticipants();
		if (cur >= max) {
			throw ApiValidationException.single("schedule_id",
					"Посещаемость занятия уже заполнена, зафиксировать услугу нельзя.");
		}
		fresh.setCurrentParticipants(cur + 1);
		scheduleSlotRepository.save(fresh);
	}

	private AvailableServiceRowDto availableMembershipRow(MembershipPurchase p, FitnessServiceEntity svc,
			ScheduleSlot slot, String details, Integer servicePurchaseId, boolean usable) {
		return AvailableServiceRowDto.builder()
				.source(SOURCE_MEMBERSHIP)
				.serviceId(svc.getServiceId())
				.serviceName(svc.getServiceName())
				.serviceType(svc.getServiceType())
				.scheduleId(slot == null ? null : slot.getScheduleId())
				.details(details)
				.membershipPurchaseId(p.getPurchaseId())
				.servicePurchaseId(servicePurchaseId)
				.usable(usable)
				.build();
	}

	private AvailableServiceRowDto availablePurchaseRow(ServicePurchaseEntity sp, FitnessServiceEntity svc,
			ScheduleSlot slot, String details, boolean usable) {
		return AvailableServiceRowDto.builder()
				.source(SOURCE_PURCHASE)
				.serviceId(svc.getServiceId())
				.serviceName(svc.getServiceName())
				.serviceType(svc.getServiceType())
				.scheduleId(slot == null ? null : slot.getScheduleId())
				.details(details)
				.membershipPurchaseId(null)
				.servicePurchaseId(sp.getPurchaseId())
				.usable(usable)
				.build();
	}

	private static String formatSlotHint(ScheduleSlot sched) {
		if (sched == null || sched.getStartDatetime() == null) {
			return "по расписанию";
		}
		LocalDateTime st = sched.getStartDatetime();
		return String.format("начало %s %02d:%02d", st.toLocalDate(), st.getHour(), st.getMinute());
	}

	private static String ruDay(LocalDate d) {
		DayOfWeek w = d.getDayOfWeek();
		return switch (w) {
			case MONDAY -> "пн";
			case TUESDAY -> "вт";
			case WEDNESDAY -> "ср";
			case THURSDAY -> "чт";
			case FRIDAY -> "пт";
			case SATURDAY -> "сб";
			case SUNDAY -> "вс";
		};
	}

	private static final class MapErr {
		private final LinkedHashMap<String, String> m = new LinkedHashMap<>();

		void add(String k, String v) {
			m.put(k, v);
		}

		void throwIfAny() {
			if (!m.isEmpty()) {
				throw new ApiValidationException(m);
			}
		}
	}
}
