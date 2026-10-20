package com.fitnessclub.service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.MembershipServiceRuleResponseDto;
import com.fitnessclub.api.dto.MembershipServiceRuleUpsertRequest;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.MembershipPlan;
import com.fitnessclub.domain.MembershipServiceLink;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.MembershipPlanRepository;
import com.fitnessclub.repository.MembershipServiceLinkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMembershipRuleService {

	private static final Set<String> ALLOWED_DAYS = Set.of("пн", "вт", "ср", "чт", "пт", "сб", "вс");

	private static final LocalTime VISIT_START_MIN = LocalTime.of(6, 0);
	private static final LocalTime VISIT_START_MAX = LocalTime.of(23, 0);
	private static final LocalTime VISIT_END_MIN = LocalTime.of(7, 0);
	private static final LocalTime VISIT_END_MAX = LocalTime.of(23, 59);

	private final MembershipServiceLinkRepository membershipServiceLinkRepository;
	private final MembershipPlanRepository membershipPlanRepository;
	private final FitnessServiceRepository fitnessServiceRepository;

	@Transactional(readOnly = true)
	public List<MembershipServiceRuleResponseDto> listAll() {
		Map<Integer, String> membershipNames = membershipPlanRepository.findAll().stream()
				.collect(Collectors.toMap(MembershipPlan::getMembershipId, MembershipPlan::getMembershipName, (a, b) -> a));
		Map<Integer, String> serviceNames = fitnessServiceRepository.findAll().stream()
				.collect(Collectors.toMap(FitnessServiceEntity::getServiceId, FitnessServiceEntity::getServiceName, (a, b) -> a));
		return membershipServiceLinkRepository.findAll().stream()
				.sorted(Comparator.comparing(MembershipServiceLink::getMembershipServiceId, Comparator.nullsLast(Comparator.naturalOrder())))
				.map(l -> toDto(l, membershipNames, serviceNames))
				.collect(Collectors.toList());
	}

	@Transactional
	public MembershipServiceRuleResponseDto upsert(MembershipServiceRuleUpsertRequest req) {
		validateRule(req);
		Integer mid = req.getMembershipId();
		Integer sid = req.getServiceId();
		MembershipServiceLink link = membershipServiceLinkRepository.findByMembershipIdAndServiceId(mid, sid)
				.orElseGet(() -> {
					MembershipServiceLink l = new MembershipServiceLink();
					l.setMembershipId(mid);
					l.setServiceId(sid);
					return l;
				});
		applyRuleFields(link, req);
		membershipServiceLinkRepository.save(link);
		return findOneDto(link.getMembershipServiceId());
	}

	@Transactional
	public MembershipServiceRuleResponseDto update(Integer membershipServiceId, MembershipServiceRuleUpsertRequest req) {
		MembershipServiceLink link = membershipServiceLinkRepository.findById(membershipServiceId)
				.orElseThrow(() -> ApiValidationException.single("membership_service_id", "Правило не найдено."));
		req.setMembershipId(link.getMembershipId());
		req.setServiceId(link.getServiceId());
		validateRule(req);
		applyRuleFields(link, req);
		membershipServiceLinkRepository.save(link);
		return findOneDto(link.getMembershipServiceId());
	}

	@Transactional(readOnly = true)
	public MembershipServiceRuleResponseDto findOneDto(Integer membershipServiceId) {
		MembershipServiceLink link = membershipServiceLinkRepository.findById(membershipServiceId)
				.orElseThrow(() -> ApiValidationException.single("membership_service_id", "Правило не найдено."));
		Map<Integer, String> membershipNames = membershipPlanRepository.findAll().stream()
				.collect(Collectors.toMap(MembershipPlan::getMembershipId, MembershipPlan::getMembershipName, (a, b) -> a));
		Map<Integer, String> serviceNames = fitnessServiceRepository.findAll().stream()
				.collect(Collectors.toMap(FitnessServiceEntity::getServiceId, FitnessServiceEntity::getServiceName, (a, b) -> a));
		return toDto(link, membershipNames, serviceNames);
	}

	@Transactional
	public void delete(Integer membershipServiceId) {
		if (!membershipServiceLinkRepository.existsById(membershipServiceId)) {
			throw ApiValidationException.single("membership_service_id", "Правило не найдено.");
		}
		membershipServiceLinkRepository.deleteById(membershipServiceId);
	}

	private void validateRule(MembershipServiceRuleUpsertRequest req) {
		Map<String, String> err = new LinkedHashMap<>();
		Integer mid = req.getMembershipId();
		Integer sid = req.getServiceId();
		if (mid == null || !membershipPlanRepository.existsById(mid)) {
			err.put("membership_id", "Укажите существующий абонемент.");
		}
		if (sid == null || !fitnessServiceRepository.existsById(sid)) {
			err.put("service_id", "Укажите существующую услугу.");
		}
		Integer iv = req.getIncludedVisits();
		if (iv != null && iv < 0) {
			err.put("included_visits", "Количество включённых посещений не может быть отрицательным.");
		}
		Integer mw = req.getMaxPerWeek();
		if (mw != null && mw < 1) {
			err.put("max_per_week", "Максимум в неделю — не меньше 1 или оставьте пустым.");
		}
		LocalTime ts = parseTimeOrNull(req.getTimeStart());
		LocalTime te = parseTimeOrNull(req.getTimeEnd());
		if ((ts == null) != (te == null)) {
			err.put("time_start", "Укажите время начала и окончания вместе или оставьте оба пустыми.");
		} else if (ts != null) {
			validateVisitTimeWindow(ts, te, err);
		}
		try {
			daysToColumn(req.getDaysOfWeek());
		} catch (ApiValidationException ex) {
			err.put("days_of_week", ex.getFieldErrors().get("days_of_week"));
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
	}

	/**
	 * Начало: 06:00–23:00. Окончание: 07:00–23:59 (позже начала в тот же день) либо 00:00 (полночь)
	 * при начале 06:00–22:59; при начале ровно в 23:00 допустимо только окончание 00:00.
	 */
	private void validateVisitTimeWindow(LocalTime ts, LocalTime te, Map<String, String> err) {
		if (ts.isBefore(VISIT_START_MIN) || ts.isAfter(VISIT_START_MAX)) {
			err.put("time_start", "Время начала: с 06:00 до 23:00.");
		}
		boolean midnightEnd = LocalTime.MIDNIGHT.equals(te);
		if (midnightEnd) {
			return;
		}
		if (ts.equals(LocalTime.of(23, 0))) {
			err.put("time_end", "При начале в 23:00 укажите окончание 00:00 (полночь).");
			return;
		}
		if (te.isBefore(VISIT_END_MIN) || te.isAfter(VISIT_END_MAX)) {
			err.put("time_end", "Время окончания: с 07:00 до 23:59 или 00:00 (полночь).");
		}
		if (!err.containsKey("time_end") && !err.containsKey("time_start") && !te.isAfter(ts)) {
			err.put("time_end", "Время окончания должно быть позже начала (в тот же календарный день).");
		}
	}

	private void applyRuleFields(MembershipServiceLink link, MembershipServiceRuleUpsertRequest req) {
		link.setIncludedVisits(req.getIncludedVisits());
		link.setMaxPerWeek(req.getMaxPerWeek());
		link.setDaysOfWeek(daysToColumn(req.getDaysOfWeek()));
		link.setTimeStart(parseTimeOrNull(req.getTimeStart()));
		link.setTimeEnd(parseTimeOrNull(req.getTimeEnd()));
	}

	private LocalTime parseTimeOrNull(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return LocalTime.parse(raw);
		} catch (DateTimeParseException e) {
			throw ApiValidationException.single("time_start", "Некорректный формат времени (ожидается ЧЧ:мм).");
		}
	}

	private String daysToColumn(List<String> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		LinkedHashSet<String> norm = new LinkedHashSet<>();
		for (String d : list) {
			if (d == null || d.isBlank()) {
				continue;
			}
			String x = d.trim().toLowerCase();
			if (!ALLOWED_DAYS.contains(x)) {
				throw ApiValidationException.single("days_of_week", "Недопустимый день недели: " + d);
			}
			norm.add(x);
		}
		if (norm.isEmpty()) {
			return null;
		}
		return String.join(",", norm);
	}

	private List<String> daysFromColumn(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}

	private String formatTime(LocalTime t) {
		if (t == null) {
			return null;
		}
		return String.format("%02d:%02d", t.getHour(), t.getMinute());
	}

	private MembershipServiceRuleResponseDto toDto(MembershipServiceLink l, Map<Integer, String> membershipNames,
			Map<Integer, String> serviceNames) {
		return MembershipServiceRuleResponseDto.builder()
				.membershipServiceId(l.getMembershipServiceId())
				.membershipId(l.getMembershipId())
				.membershipName(membershipNames.getOrDefault(l.getMembershipId(), "—"))
				.serviceId(l.getServiceId())
				.serviceName(serviceNames.getOrDefault(l.getServiceId(), "—"))
				.includedVisits(l.getIncludedVisits())
				.maxPerWeek(l.getMaxPerWeek())
				.daysOfWeek(daysFromColumn(l.getDaysOfWeek()))
				.timeStart(formatTime(l.getTimeStart()))
				.timeEnd(formatTime(l.getTimeEnd()))
				.build();
	}
}
