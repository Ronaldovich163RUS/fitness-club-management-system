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

import com.fitnessclub.api.dto.ScheduleResponseDto;
import com.fitnessclub.api.dto.ScheduleUpsertRequest;
import com.fitnessclub.service.AdminScheduleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleRestController {

	private final AdminScheduleService scheduleService;

	@GetMapping
	public List<ScheduleResponseDto> list() {
		return scheduleService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ScheduleResponseDto create(@RequestBody ScheduleUpsertRequest body) {
		return scheduleService.create(body);
	}

	@PutMapping("/{scheduleId}")
	public ScheduleResponseDto update(@PathVariable Integer scheduleId, @RequestBody ScheduleUpsertRequest body) {
		return scheduleService.update(scheduleId, body);
	}

	@DeleteMapping("/{scheduleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer scheduleId) {
		scheduleService.delete(scheduleId);
	}
}
