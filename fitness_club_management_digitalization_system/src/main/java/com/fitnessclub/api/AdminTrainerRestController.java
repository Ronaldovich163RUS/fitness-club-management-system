package com.fitnessclub.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.api.dto.DocumentRequestDto;
import com.fitnessclub.api.dto.DocumentResponseDto;
import com.fitnessclub.api.dto.TrainerCreateRequest;
import com.fitnessclub.api.dto.TrainerResponseDto;
import com.fitnessclub.api.dto.TrainerUpdateRequest;
import com.fitnessclub.service.AdminTrainerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/trainers")
@RequiredArgsConstructor
public class AdminTrainerRestController {

	private final AdminTrainerService trainerService;

	@GetMapping
	public List<TrainerResponseDto> list() {
		return trainerService.listAll();
	}

	@GetMapping("/clients-available")
	public List<Map<String, Object>> availableClients() {
		return trainerService.availableClientsForTrainerRole();
	}

	@PostMapping
	public ResponseEntity<TrainerResponseDto> create(@RequestBody TrainerCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(trainerService.create(request));
	}

	@PostMapping("/from-client/{clientId}")
	public ResponseEntity<TrainerResponseDto> createFromClient(@PathVariable Integer clientId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(trainerService.createFromClient(clientId));
	}

	@PutMapping("/{id}")
	public TrainerResponseDto update(@PathVariable("id") Integer id, @RequestBody TrainerUpdateRequest request) {
		return trainerService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("id") Integer id) {
		trainerService.delete(id);
	}

	@GetMapping("/{id}/documents")
	public List<DocumentResponseDto> listDocuments(@PathVariable("id") Integer trainerId) {
		return trainerService.listDocuments(trainerId);
	}

	@PostMapping("/{id}/documents")
	public ResponseEntity<DocumentResponseDto> createDocument(
			@PathVariable("id") Integer trainerId,
			@RequestBody DocumentRequestDto dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(trainerService.createDocument(trainerId, dto));
	}

	@PutMapping("/{id}/documents/{docId}")
	public DocumentResponseDto updateDocument(
			@PathVariable("id") Integer trainerId,
			@PathVariable("docId") Integer docId,
			@RequestBody DocumentRequestDto dto) {
		return trainerService.updateDocument(trainerId, docId, dto);
	}

	@DeleteMapping("/{id}/documents/{docId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDocument(@PathVariable("id") Integer trainerId, @PathVariable("docId") Integer docId) {
		trainerService.deleteDocument(trainerId, docId);
	}
}
