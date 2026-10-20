package com.fitnessclub.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.MembershipBriefDto;
import com.fitnessclub.api.dto.ServiceResponseDto;
import com.fitnessclub.api.dto.ServiceUpsertRequest;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.MembershipServiceLink;
import com.fitnessclub.domain.Zone;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.MembershipPlanRepository;
import com.fitnessclub.repository.MembershipServiceLinkRepository;
import com.fitnessclub.repository.ScheduleSlotRepository;
import com.fitnessclub.repository.ServicePurchaseEntityRepository;
import com.fitnessclub.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceCatalogService {

	private static final int NAME_MIN = 1;
	private static final int NAME_MAX = 200;
	private static final int DESC_MAX = 2000;
	private static final int DURATION_MIN = 5;
	private static final int DURATION_MAX = 600;
	private static final BigDecimal PRICE_MAX = new BigDecimal("999999.99");

	private static final Set<String> SERVICE_TYPES = Set.of(
			"групповая тренировка",
			"персональная тренировка",
			"доступ в зону",
			"дополнительная услуга");

	private final FitnessServiceRepository serviceRepository;
	private final ZoneRepository zoneRepository;
	private final MembershipPlanRepository membershipPlanRepository;
	private final MembershipServiceLinkRepository membershipServiceLinkRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final ServicePurchaseEntityRepository servicePurchaseEntityRepository;
	private final CurrentAdminIdResolver adminIdResolver;

	@Transactional(readOnly = true)
	public List<MembershipBriefDto> listMembershipsBrief() {
		return membershipPlanRepository.findAllByOrderByMembershipNameAsc().stream()
				.filter(m -> m.getIsActive() == null || Boolean.TRUE.equals(m.getIsActive()))
				.map(m -> MembershipBriefDto.builder()
						.membershipId(m.getMembershipId())
						.membershipName(m.getMembershipName())
						.build())
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<ServiceResponseDto> listAll() {
		return serviceRepository.findAllFetchZone().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public ServiceResponseDto create(ServiceUpsertRequest req) {
		validate(req, null);
		FitnessServiceEntity e = new FitnessServiceEntity();
		apply(e, req);
		e.setCreatedByAdminId(adminIdResolver.requireDefaultAdminId());
		serviceRepository.save(e);
		replaceMembershipLinks(e.getServiceId(), req.getMembershipIds());
		return toDto(e);
	}

	@Transactional
	public ServiceResponseDto update(Integer serviceId, ServiceUpsertRequest req) {
		FitnessServiceEntity e = serviceRepository.findById(serviceId)
				.orElseThrow(() -> ApiValidationException.single("service_id", "Услуга не найдена."));
		validate(req, serviceId);
		apply(e, req);
		replaceMembershipLinks(serviceId, req.getMembershipIds());
		return toDto(e);
	}

	@Transactional
	public void delete(Integer serviceId) {
		if (!serviceRepository.existsById(serviceId)) {
			throw ApiValidationException.single("service_id", "Услуга не найдена.");
		}
		if (scheduleSlotRepository.existsByService_ServiceId(serviceId)) {
			throw ApiValidationException.single("service_id", "Нельзя удалить услугу: есть занятия в расписании.");
		}
		if (servicePurchaseEntityRepository.existsByService_ServiceId(serviceId)) {
			throw ApiValidationException.single("service_id",
					"Нельзя удалить услугу: в системе есть разовые покупки этой услуги клиентами (нельзя нарушить историю).");
		}
		membershipServiceLinkRepository.deleteByServiceId(serviceId);
		serviceRepository.deleteById(serviceId);
	}

	private void validate(ServiceUpsertRequest req, Integer excludeServiceId) {
		Map<String, String> err = new LinkedHashMap<>();
		String name = req.getServiceName() == null ? "" : req.getServiceName().trim();
		if (name.length() < NAME_MIN || name.length() > NAME_MAX) {
			err.put("service_name", "Название услуги: от " + NAME_MIN + " до " + NAME_MAX + " символов.");
		}
		String type = req.getServiceType() == null ? "" : req.getServiceType().trim();
		if (!SERVICE_TYPES.contains(type)) {
			err.put("service_type", "Недопустимый тип услуги.");
		}
		Integer dur = req.getDurationMinutes();
		if (dur == null || dur < DURATION_MIN || dur > DURATION_MAX) {
			err.put("duration_minutes", "Длительность: от " + DURATION_MIN + " до " + DURATION_MAX + " минут.");
		}
		BigDecimal price = req.getBasePrice();
		if (price == null || price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(PRICE_MAX) > 0) {
			err.put("base_price", "Цена: от 0 до " + PRICE_MAX + ".");
		}
		String desc = req.getDescription() == null ? "" : req.getDescription().trim();
		if (desc.isEmpty()) {
			err.put("description", "Описание обязательно.");
		} else if (desc.length() > DESC_MAX) {
			err.put("description", "Описание не длиннее " + DESC_MAX + " символов.");
		}
		Integer zid = req.getZoneId();
		if (zid == null || !zoneRepository.existsById(zid)) {
			err.put("zone_id", "Укажите существующую зону.");
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
		if (excludeServiceId == null) {
			if (serviceRepository.existsByServiceNameIgnoreCase(name)) {
				throw ApiValidationException.single("service_name", "Услуга с таким названием уже есть.");
			}
		} else {
			if (serviceRepository.existsByServiceNameIgnoreCaseAndServiceIdNot(name, excludeServiceId)) {
				throw ApiValidationException.single("service_name", "Услуга с таким названием уже есть.");
			}
		}
		if ("персональная тренировка".equals(type)) {
			boolean personalExists = excludeServiceId == null
					? serviceRepository.existsPersonalByZoneId(zid)
					: serviceRepository.existsPersonalByZoneIdExcluding(zid, excludeServiceId);
			if (personalExists) {
				throw ApiValidationException.single("zone_id",
						"В выбранной зоне уже есть услуга типа «персональная тренировка». Допустима только одна.");
			}
		}
		List<Integer> mids = req.getMembershipIds();
		if (mids != null) {
			for (Integer mid : new LinkedHashSet<>(mids)) {
				if (mid != null && !membershipPlanRepository.existsById(mid)) {
					throw ApiValidationException.single("membership_ids", "Недопустимый тип абонемента: " + mid);
				}
			}
		}
	}

	private void apply(FitnessServiceEntity e, ServiceUpsertRequest req) {
		e.setServiceName(req.getServiceName().trim());
		e.setServiceType(req.getServiceType().trim());
		e.setDurationMinutes(req.getDurationMinutes());
		e.setDescription(req.getDescription().trim());
		e.setBasePrice(req.getBasePrice());
		Zone z = zoneRepository.findById(req.getZoneId()).orElseThrow();
		e.setZone(z);
		// По умолчанию новая/обновлённая услуга считается активной, если флаг не передан.
		e.setIsActive(req.getIsActive() == null ? Boolean.TRUE : Boolean.TRUE.equals(req.getIsActive()));
	}

	private void replaceMembershipLinks(Integer serviceId, List<Integer> membershipIds) {
		membershipServiceLinkRepository.deleteByServiceId(serviceId);
		if (membershipIds == null || membershipIds.isEmpty()) {
			return;
		}
		for (Integer mid : membershipIds.stream().filter(m -> m != null).collect(Collectors.toCollection(LinkedHashSet::new))) {
			MembershipServiceLink l = new MembershipServiceLink();
			l.setMembershipId(mid);
			l.setServiceId(serviceId);
			membershipServiceLinkRepository.save(l);
		}
	}

	private ServiceResponseDto toDto(FitnessServiceEntity s) {
		Zone z = s.getZone();
		List<Integer> mids = membershipServiceLinkRepository.findByServiceId(s.getServiceId()).stream()
				.map(MembershipServiceLink::getMembershipId)
				.collect(Collectors.toList());
		return ServiceResponseDto.builder()
				.serviceId(s.getServiceId())
				.serviceName(s.getServiceName())
				.serviceType(s.getServiceType())
				.durationMinutes(s.getDurationMinutes())
				.description(s.getDescription())
				.basePrice(s.getBasePrice())
				.zoneId(z == null ? null : z.getZoneId())
				.zoneName(z == null ? null : z.getZoneName())
				.isActive(Boolean.TRUE.equals(s.getIsActive()))
				.membershipIds(mids)
				.build();
	}
}
