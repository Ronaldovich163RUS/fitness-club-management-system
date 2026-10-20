package com.fitnessclub.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.api.dto.ZoneResponseDto;
import com.fitnessclub.api.dto.ZoneUpsertRequest;
import com.fitnessclub.service.AdminZoneService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/zones")
@RequiredArgsConstructor
public class AdminZoneRestController {

	private final AdminZoneService zoneService;

	@GetMapping
	public List<ZoneResponseDto> list() {
		return zoneService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ZoneResponseDto create(@RequestBody ZoneUpsertRequest body) {
		return zoneService.create(body);
	}

	@PutMapping("/{zoneId}")
	public ZoneResponseDto update(@PathVariable Integer zoneId, @RequestBody ZoneUpsertRequest body) {
		return zoneService.update(zoneId, body);
	}

	@DeleteMapping("/{zoneId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer zoneId) {
		zoneService.delete(zoneId);
	}
}
