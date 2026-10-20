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

import com.fitnessclub.api.dto.ServiceResponseDto;
import com.fitnessclub.api.dto.ServiceUpsertRequest;
import com.fitnessclub.service.AdminServiceCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
public class AdminFitnessServiceRestController {

	private final AdminServiceCatalogService catalogService;

	@GetMapping
	public List<ServiceResponseDto> list() {
		return catalogService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ServiceResponseDto create(@RequestBody ServiceUpsertRequest body) {
		return catalogService.create(body);
	}

	@PutMapping("/{serviceId}")
	public ServiceResponseDto update(@PathVariable Integer serviceId, @RequestBody ServiceUpsertRequest body) {
		return catalogService.update(serviceId, body);
	}

	@DeleteMapping("/{serviceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Integer serviceId) {
		catalogService.delete(serviceId);
	}
}
