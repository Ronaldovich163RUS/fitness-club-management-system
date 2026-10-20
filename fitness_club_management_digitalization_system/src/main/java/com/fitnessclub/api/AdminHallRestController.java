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

import com.fitnessclub.api.dto.HallResponseDto;
import com.fitnessclub.api.dto.HallUpsertRequest;
import com.fitnessclub.service.AdminHallService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
public class AdminHallRestController {

	private final AdminHallService hallService;

	@GetMapping
	public List<HallResponseDto> list() {
		return hallService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public HallResponseDto create(@RequestBody HallUpsertRequest body) {
		return hallService.create(body);
	}

	@PutMapping("/{hallId}")
	public HallResponseDto update(@PathVariable Integer hallId, @RequestBody HallUpsertRequest body) {
		return hallService.update(hallId, body);
	}

	@DeleteMapping("/{hallId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer hallId) {
		hallService.delete(hallId);
	}
}
