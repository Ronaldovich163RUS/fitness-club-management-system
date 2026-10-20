package com.fitnessclub.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.MembershipPlanResponseDto;
import com.fitnessclub.api.dto.MembershipPlanUpsertRequest;
import com.fitnessclub.domain.MembershipPlan;
import com.fitnessclub.domain.MembershipServiceLink;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.MembershipPlanRepository;
import com.fitnessclub.repository.MembershipServiceLinkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMembershipPlanService {

	private static final int NAME_MAX = 50;
	private static final int DESC_MAX = 100;
	private static final BigDecimal PRICE_MAX = new BigDecimal("1000000");
	private static final int DURATION_MAX_DAYS = 5 * 365;
	private static final int FREEZE_MAX_DAYS = 365;

	private static final Pattern NAME_ALLOWED = Pattern.compile("^[\\p{L}\\p{N}]+$");
	/** Буквы, цифры, пробелы и знаки . , ; : */
	private static final Pattern DESCRIPTION_ALLOWED = Pattern.compile("^[\\p{L}\\p{N}\\s.,;:]+$");

	private final MembershipPlanRepository membershipPlanRepository;
	private final MembershipServiceLinkRepository membershipServiceLinkRepository;
	private final FitnessServiceRepository fitnessServiceRepository;
	private final CurrentAdminIdResolver adminIdResolver;

	@Transactional(readOnly = true)
	public List<MembershipPlanResponseDto> listAll() {
		return membershipPlanRepository.findAllByOrderByMembershipNameAsc().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public MembershipPlanResponseDto create(MembershipPlanUpsertRequest req) {
		validateFields(req);
		String name = req.getMembershipName() == null ? "" : req.getMembershipName().trim();
		Optional<MembershipPlan> existing = membershipPlanRepository.findFirstByMembershipNameIgnoreCase(name);
		if (existing.isPresent()) {
			return update(existing.get().getMembershipId(), req);
		}
		MembershipPlan m = new MembershipPlan();
		apply(m, req);
		m.setCreatedByAdminId(adminIdResolver.requireDefaultAdminId());
		membershipPlanRepository.save(m);
		replaceServiceLinks(m.getMembershipId(), req.getServiceIds());
		return toDto(membershipPlanRepository.findById(m.getMembershipId()).orElse(m));
	}

	@Transactional
	public MembershipPlanResponseDto update(Integer membershipId, MembershipPlanUpsertRequest req) {
		MembershipPlan m = membershipPlanRepository.findById(membershipId)
				.orElseThrow(() -> ApiValidationException.single("membership_id", "Абонемент не найден."));
		validateFields(req);
		assertUniqueMembershipName(req, membershipId);
		apply(m, req);
		replaceServiceLinks(membershipId, req.getServiceIds());
		return toDto(m);
	}

	@Transactional
	public void delete(Integer membershipId) {
		if (!membershipPlanRepository.existsById(membershipId)) {
			throw ApiValidationException.single("membership_id", "Абонемент не найден.");
		}
		membershipServiceLinkRepository.deleteByMembershipId(membershipId);
		membershipPlanRepository.deleteById(membershipId);
	}

	/** Поля запроса без проверки уникальности названия (она отдельно для create/update). */
	private void validateFields(MembershipPlanUpsertRequest req) {
		Map<String, String> err = new LinkedHashMap<>();
		String name = req.getMembershipName() == null ? "" : req.getMembershipName().trim();
		if (name.isEmpty() || name.length() > NAME_MAX) {
			err.put("membership_name", "Название обязательно, до " + NAME_MAX + " символов, только буквы и цифры.");
		} else if (!NAME_ALLOWED.matcher(name).matches()) {
			err.put("membership_name", "В названии допустимы только буквы и цифры.");
		}
		String desc = req.getDescription() == null ? "" : req.getDescription().trim();
		if (desc.isEmpty()) {
			err.put("description", "Описание обязательно.");
		} else if (desc.length() > DESC_MAX) {
			err.put("description", "Описание не длиннее " + DESC_MAX + " символов.");
		} else if (!DESCRIPTION_ALLOWED.matcher(desc).matches()) {
			err.put("description", "В описании допустимы буквы, цифры, пробелы и знаки . , ; :");
		}
		BigDecimal price = req.getPrice();
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(PRICE_MAX) > 0) {
			err.put("price", "Цена больше 0 и не больше 1 000 000 руб.");
		}
		Integer dur = req.getDurationDays();
		if (dur == null || dur < 1) {
			err.put("duration_days", "Срок действия — не меньше 1 дня.");
		} else if (dur > DURATION_MAX_DAYS) {
			err.put("duration_days", "Максимальный срок — 5 лет (" + DURATION_MAX_DAYS + " дн.).");
		}
		Integer vl = req.getVisitsLimit();
		if (vl != null && vl < 0) {
			err.put("visits_limit", "Лимит посещений не может быть отрицательным.");
		}
		Integer fd = req.getFreezeDays();
		if (fd != null && fd < 0) {
			err.put("freeze_days", "Дни заморозки не могут быть отрицательными.");
		} else if (fd != null && fd > FREEZE_MAX_DAYS) {
			err.put("freeze_days", "Заморозка не больше 1 года (" + FREEZE_MAX_DAYS + " дн.).");
		}
		Integer gv = req.getGuestVisits();
		if (gv != null && gv < 0) {
			err.put("guest_visits", "Гостевые визиты не могут быть отрицательными.");
		}
		if (dur != null && fd != null && fd >= dur) {
			err.put("freeze_days", "Срок заморозки должен быть меньше срока действия абонемента.");
		}
		if (vl != null && gv != null && gv > vl) {
			err.put("guest_visits", "Гостевые визиты не больше лимита посещений по абонементу.");
		}
		List<Integer> ids = req.getServiceIds();
		if (ids != null) {
			for (Integer sid : new LinkedHashSet<>(ids)) {
				if (sid != null && !fitnessServiceRepository.existsById(sid)) {
					err.put("service_ids", "Неизвестная услуга: " + sid);
					break;
				}
			}
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
	}

	private void assertUniqueMembershipName(MembershipPlanUpsertRequest req, Integer excludeMembershipId) {
		String name = req.getMembershipName() == null ? "" : req.getMembershipName().trim();
		if (membershipPlanRepository.existsByMembershipNameIgnoreCaseAndMembershipIdNot(name, excludeMembershipId)) {
			throw ApiValidationException.single("membership_name", "Абонемент с таким названием уже есть.");
		}
	}

	private void apply(MembershipPlan m, MembershipPlanUpsertRequest req) {
		m.setMembershipName(req.getMembershipName().trim());
		m.setDescription(req.getDescription().trim());
		m.setPrice(req.getPrice());
		m.setDurationDays(req.getDurationDays());
		m.setVisitsLimit(req.getVisitsLimit());
		m.setFreezeDays(req.getFreezeDays());
		m.setGuestVisits(req.getGuestVisits());
		m.setParking(Boolean.TRUE.equals(req.getParking()) ? Boolean.TRUE : Boolean.FALSE);
		m.setTowel(Boolean.TRUE.equals(req.getTowel()) ? Boolean.TRUE : Boolean.FALSE);
		m.setIsActive(Boolean.TRUE.equals(req.getIsActive()) ? Boolean.TRUE : Boolean.FALSE);
	}

	private void replaceServiceLinks(Integer membershipId, List<Integer> serviceIds) {
		List<MembershipServiceLink> existing = membershipServiceLinkRepository
				.findByMembershipIdOrderByServiceIdAsc(membershipId);
		Set<Integer> want = serviceIds == null ? Set.of()
				: serviceIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
		List<MembershipServiceLink> toRemove = existing.stream()
				.filter(link -> !want.contains(link.getServiceId()))
				.collect(Collectors.toList());
		for (MembershipServiceLink link : toRemove) {
			membershipServiceLinkRepository.delete(link);
		}
		Set<Integer> stillThere = existing.stream()
				.filter(link -> want.contains(link.getServiceId()))
				.map(MembershipServiceLink::getServiceId)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		for (Integer sid : want) {
			if (!stillThere.contains(sid)) {
				MembershipServiceLink l = new MembershipServiceLink();
				l.setMembershipId(membershipId);
				l.setServiceId(sid);
				membershipServiceLinkRepository.save(l);
				stillThere.add(sid);
			}
		}
	}

	private MembershipPlanResponseDto toDto(MembershipPlan m) {
		List<Integer> sids = membershipServiceLinkRepository.findByMembershipIdOrderByServiceIdAsc(m.getMembershipId())
				.stream()
				.map(MembershipServiceLink::getServiceId)
				.collect(Collectors.toList());
		return MembershipPlanResponseDto.builder()
				.membershipId(m.getMembershipId())
				.membershipName(m.getMembershipName())
				.description(m.getDescription())
				.price(m.getPrice())
				.durationDays(m.getDurationDays())
				.visitsLimit(m.getVisitsLimit())
				.freezeDays(m.getFreezeDays())
				.guestVisits(m.getGuestVisits())
				.parking(Boolean.TRUE.equals(m.getParking()))
				.towel(Boolean.TRUE.equals(m.getTowel()))
				.isActive(!Boolean.FALSE.equals(m.getIsActive()))
				.serviceIds(sids)
				.build();
	}
}
