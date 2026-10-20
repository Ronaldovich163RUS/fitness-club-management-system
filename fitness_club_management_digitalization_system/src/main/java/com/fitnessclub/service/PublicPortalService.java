package com.fitnessclub.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.MembershipPlan;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.MembershipPlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicPortalService {

	private final MembershipPlanRepository membershipPlanRepository;
	private final FitnessServiceRepository fitnessServiceRepository;

	@Transactional(readOnly = true)
	public List<Map<String, Object>> membershipCatalogAll() {
		return membershipPlanRepository.findAllByOrderByMembershipNameAsc().stream()
				.filter(m -> Boolean.TRUE.equals(m.getIsActive()))
				.map(this::membershipRow)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> serviceCatalogAll() {
		return fitnessServiceRepository.findAllFetchZone().stream()
				.filter(s -> Boolean.TRUE.equals(s.getIsActive()))
				.map(this::serviceRow)
				.collect(Collectors.toList());
	}

	private Map<String, Object> membershipRow(MembershipPlan m) {
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
		x.put("is_active", Boolean.TRUE.equals(m.getIsActive()));
		return x;
	}

	private Map<String, Object> serviceRow(FitnessServiceEntity s) {
		Map<String, Object> x = new LinkedHashMap<>();
		x.put("service_id", s.getServiceId());
		x.put("service_name", s.getServiceName());
		x.put("service_type", s.getServiceType());
		x.put("duration_minutes", s.getDurationMinutes());
		x.put("description", s.getDescription());
		x.put("base_price", s.getBasePrice());
		x.put("zone_name", s.getZone() == null ? null : s.getZone().getZoneName());
		x.put("is_active", Boolean.TRUE.equals(s.getIsActive()));
		return x;
	}
}
