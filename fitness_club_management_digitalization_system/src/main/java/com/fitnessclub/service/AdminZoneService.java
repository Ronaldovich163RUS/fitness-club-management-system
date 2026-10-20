package com.fitnessclub.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.ZoneResponseDto;
import com.fitnessclub.api.dto.ZoneUpsertRequest;
import com.fitnessclub.domain.Zone;
import com.fitnessclub.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminZoneService {

	private static final int NAME_MAX = 50;
	private static final int DESC_MAX = 1000;

	private final ZoneRepository zoneRepository;

	@Transactional(readOnly = true)
	public List<ZoneResponseDto> listAll() {
		return zoneRepository.findAll().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public ZoneResponseDto create(ZoneUpsertRequest req) {
		validate(req, null);
		Zone z = new Zone();
		apply(z, req);
		zoneRepository.save(z);
		return toDto(z);
	}

	@Transactional
	public ZoneResponseDto update(Integer zoneId, ZoneUpsertRequest req) {
		Zone z = zoneRepository.findById(zoneId)
				.orElseThrow(() -> ApiValidationException.single("zone_id", "Зона не найдена."));
		validate(req, zoneId);
		apply(z, req);
		return toDto(z);
	}

	@Transactional
	public void delete(Integer zoneId) {
		if (!zoneRepository.existsById(zoneId)) {
			throw ApiValidationException.single("zone_id", "Зона не найдена.");
		}
		zoneRepository.deleteById(zoneId);
	}

	private void validate(ZoneUpsertRequest req, Integer excludeZoneId) {
		Map<String, String> err = new LinkedHashMap<>();
		String name = req.getZoneName() == null ? "" : req.getZoneName().trim();
		if (name.isEmpty() || name.length() > NAME_MAX) {
			err.put("zone_name", "Название зоны обязательно, до " + NAME_MAX + " символов.");
		}
		String desc = normalizeDescription(req.getDescription());
		if (desc != null && desc.length() > DESC_MAX) {
			err.put("description", "Описание не длиннее " + DESC_MAX + " символов.");
		}
		if (err.isEmpty() && !name.isEmpty()) {
			boolean dup = excludeZoneId == null
					? zoneRepository.existsByZoneNameIgnoreCase(name)
					: zoneRepository.existsByZoneNameIgnoreCaseAndZoneIdNot(name, excludeZoneId);
			if (dup) {
				err.put("zone_name", "Зона с таким названием уже существует.");
			}
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
	}

	private void apply(Zone z, ZoneUpsertRequest req) {
		z.setZoneName(req.getZoneName().trim());
		z.setDescription(normalizeDescription(req.getDescription()));
	}

	private String normalizeDescription(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw.trim();
	}

	private ZoneResponseDto toDto(Zone z) {
		return ZoneResponseDto.builder()
				.zoneId(z.getZoneId())
				.zoneName(z.getZoneName())
				.description(z.getDescription())
				.build();
	}
}
