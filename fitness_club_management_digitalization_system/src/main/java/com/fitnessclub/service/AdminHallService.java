package com.fitnessclub.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.HallResponseDto;
import com.fitnessclub.api.dto.HallUpsertRequest;
import com.fitnessclub.domain.Hall;
import com.fitnessclub.domain.Zone;
import com.fitnessclub.repository.HallRepository;
import com.fitnessclub.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminHallService {

	private static final int NAME_MAX = 100;
	private static final int DESC_MAX = 1000;

	private final HallRepository hallRepository;
	private final ZoneRepository zoneRepository;
	private final CurrentAdminIdResolver adminIdResolver;

	@Transactional(readOnly = true)
	public List<HallResponseDto> listAll() {
		return hallRepository.findAllFetchZone().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public HallResponseDto create(HallUpsertRequest req) {
		validate(req, null);
		Hall h = new Hall();
		apply(h, req);
		h.setCreatedByAdminId(adminIdResolver.requireDefaultAdminId());
		hallRepository.save(h);
		return toDto(h);
	}

	@Transactional
	public HallResponseDto update(Integer hallId, HallUpsertRequest req) {
		Hall h = hallRepository.findById(hallId)
				.orElseThrow(() -> ApiValidationException.single("hall_id", "Зал не найден."));
		validate(req, hallId);
		apply(h, req);
		return toDto(h);
	}

	@Transactional
	public void delete(Integer hallId) {
		if (!hallRepository.existsById(hallId)) {
			throw ApiValidationException.single("hall_id", "Зал не найден.");
		}
		hallRepository.deleteById(hallId);
	}

	private void validate(HallUpsertRequest req, Integer excludeHallId) {
		Map<String, String> err = new LinkedHashMap<>();
		String name = req.getHallName() == null ? "" : req.getHallName().trim();
		if (name.isEmpty() || name.length() > NAME_MAX) {
			err.put("hall_name", "Название зала обязательно, до " + NAME_MAX + " символов.");
		}
		Integer cap = req.getCapacity();
		if (cap == null || cap < 1) {
			err.put("capacity", "Вместимость — целое число не меньше 1.");
		}
		String desc = normalizeDescription(req.getDescription());
		if (desc != null && desc.length() > DESC_MAX) {
			err.put("description", "Описание не длиннее " + DESC_MAX + " символов.");
		}
		Integer zid = req.getZoneId();
		if (zid != null && !zoneRepository.existsById(zid)) {
			err.put("zone_id", "Зона с таким идентификатором не найдена.");
		}
		if (err.isEmpty() && !name.isEmpty()) {
			boolean dup = excludeHallId == null
					? hallRepository.existsByHallNameIgnoreCase(name)
					: hallRepository.existsByHallNameIgnoreCaseAndHallIdNot(name, excludeHallId);
			if (dup) {
				err.put("hall_name", "Зал с таким названием уже существует.");
			}
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
	}

	private void apply(Hall h, HallUpsertRequest req) {
		h.setHallName(req.getHallName().trim());
		h.setCapacity(req.getCapacity());
		h.setDescription(normalizeDescription(req.getDescription()));
		if (req.getIsActive() == null) {
			h.setIsActive(Boolean.TRUE);
		} else {
			h.setIsActive(Boolean.TRUE.equals(req.getIsActive()));
		}
		Integer zid = req.getZoneId();
		if (zid == null) {
			h.setZone(null);
		} else {
			Zone z = zoneRepository.findById(zid).orElseThrow();
			h.setZone(z);
		}
	}

	private String normalizeDescription(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return raw.trim();
	}

	private HallResponseDto toDto(Hall h) {
		Zone z = h.getZone();
		return HallResponseDto.builder()
				.hallId(h.getHallId())
				.hallName(h.getHallName())
				.zoneId(z == null ? null : z.getZoneId())
				.zoneName(z == null ? null : z.getZoneName())
				.capacity(h.getCapacity())
				.description(h.getDescription())
				.isActive(!Boolean.FALSE.equals(h.getIsActive()))
				.createdByAdminId(h.getCreatedByAdminId())
				.build();
	}
}
