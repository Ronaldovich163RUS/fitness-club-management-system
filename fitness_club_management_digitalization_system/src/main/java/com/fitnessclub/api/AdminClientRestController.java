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

import com.fitnessclub.api.dto.ClientCreateRequest;
import com.fitnessclub.api.dto.ClientResponseDto;
import com.fitnessclub.api.dto.ClientUpdateRequest;
import com.fitnessclub.api.dto.DocumentRequestDto;
import com.fitnessclub.api.dto.DocumentResponseDto;
import com.fitnessclub.service.AdminClientService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientRestController {

	private final AdminClientService clientService;

	@GetMapping
	public List<ClientResponseDto> list() {
		return clientService.listAll();
	}

	@GetMapping("/trainers-available")
	public List<Map<String, Object>> availableTrainers() {
		return clientService.availableTrainersForClientRole();
	}

	@PostMapping
	public ResponseEntity<ClientResponseDto> create(@RequestBody ClientCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
	}

	@PostMapping("/from-trainer/{trainerId}")
	public ResponseEntity<ClientResponseDto> createFromTrainer(@PathVariable Integer trainerId) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createFromTrainer(trainerId));
	}

	@PutMapping("/{id}")
	public ClientResponseDto update(@PathVariable("id") Integer id, @RequestBody ClientUpdateRequest request) {
		return clientService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("id") Integer id) {
		clientService.delete(id);
	}

	@GetMapping("/{id}/documents")
	public List<DocumentResponseDto> listDocuments(@PathVariable("id") Integer clientId) {
		return clientService.listDocuments(clientId);
	}

	@PostMapping("/{id}/documents")
	public ResponseEntity<DocumentResponseDto> createDocument(
			@PathVariable("id") Integer clientId,
			@RequestBody DocumentRequestDto dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createDocument(clientId, dto));
	}

	@PutMapping("/{id}/documents/{docId}")
	public DocumentResponseDto updateDocument(
			@PathVariable("id") Integer clientId,
			@PathVariable("docId") Integer docId,
			@RequestBody DocumentRequestDto dto) {
		return clientService.updateDocument(clientId, docId, dto);
	}

	@DeleteMapping("/{id}/documents/{docId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDocument(@PathVariable("id") Integer clientId, @PathVariable("docId") Integer docId) {
		clientService.deleteDocument(clientId, docId);
	}

	@GetMapping("/{id}/membership")
	public Map<String, Object> membershipOverview(@PathVariable("id") Integer clientId) {
		return clientService.membershipOverview(clientId);
	}

	@PostMapping("/{id}/membership/freeze")
	public Map<String, Object> freezeMembership(@PathVariable("id") Integer clientId, @RequestBody Map<String, Object> body) {
		Integer days = body.get("days") == null ? null : Integer.valueOf(String.valueOf(body.get("days")));
		return clientService.freezeMembership(clientId, days);
	}

	@PostMapping("/{id}/membership/unfreeze")
	public Map<String, Object> unfreezeMembership(@PathVariable("id") Integer clientId) {
		return clientService.unfreezeMembership(clientId);
	}

	@DeleteMapping("/{id}/membership")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void annulMembership(@PathVariable("id") Integer clientId) {
		clientService.annulMembership(clientId);
	}
}
