package com.fitnessclub.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.service.ClientPortalService;
import com.fitnessclub.service.PublicPortalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicPortalRestController {

	private final ClientPortalService clientPortalService;
	private final PublicPortalService publicPortalService;

	@GetMapping("/training-types")
	public List<Map<String, Object>> trainingTypes() {
		return clientPortalService.trainingTypes();
	}

	@GetMapping("/trainers")
	public List<Map<String, Object>> trainers() {
		return clientPortalService.trainers();
	}

	@GetMapping("/schedule/group")
	public List<Map<String, Object>> scheduleGroup() {
		return clientPortalService.scheduleGroup();
	}

	@GetMapping("/memberships/catalog")
	public List<Map<String, Object>> membershipCatalog() {
		return publicPortalService.membershipCatalogAll();
	}

	@GetMapping("/memberships/{membershipId}/rules")
	public List<Map<String, Object>> membershipRules(@PathVariable Integer membershipId) {
		return clientPortalService.membershipRules(membershipId);
	}

	@GetMapping("/services/catalog")
	public List<Map<String, Object>> serviceCatalog() {
		return publicPortalService.serviceCatalogAll();
	}

	@GetMapping("/services/{serviceId}/rules")
	public Map<String, Object> serviceRules(@PathVariable Integer serviceId) {
		return clientPortalService.serviceRulesOneRow(serviceId);
	}
}
