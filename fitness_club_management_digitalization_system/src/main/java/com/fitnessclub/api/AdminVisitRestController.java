package com.fitnessclub.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.api.dto.AvailableServiceRowDto;
import com.fitnessclub.api.dto.ClubStatsResponseDto;
import com.fitnessclub.api.dto.OpenVisitClientDto;
import com.fitnessclub.api.dto.UseServiceRequest;
import com.fitnessclub.api.dto.VisitEntryRequest;
import com.fitnessclub.api.dto.VisitExitRequest;
import com.fitnessclub.api.dto.VisitJournalRowDto;
import com.fitnessclub.service.AdminVisitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/visits")
@RequiredArgsConstructor
public class AdminVisitRestController {

	private final AdminVisitService visitService;

	@GetMapping("/club-stats")
	public ClubStatsResponseDto clubStats() {
		return visitService.clubStats();
	}

	@GetMapping("/journal")
	public List<VisitJournalRowDto> journal() {
		return visitService.journal();
	}

	@GetMapping("/open-clients")
	public List<OpenVisitClientDto> openClients() {
		return visitService.openVisitClients();
	}

	@GetMapping("/available-services/{clientId}")
	public List<AvailableServiceRowDto> availableServices(@PathVariable("clientId") Integer clientId) {
		return visitService.availableServices(clientId);
	}

	@PostMapping("/entry")
	@ResponseStatus(HttpStatus.CREATED)
	public void entry(@RequestBody VisitEntryRequest body) {
		visitService.registerEntry(body);
	}

	@PostMapping("/exit")
	public void exit(@RequestBody VisitExitRequest body) {
		visitService.registerExit(body);
	}

	@PostMapping("/use-service")
	public void useService(@RequestBody UseServiceRequest body) {
		visitService.useService(body);
	}
}
