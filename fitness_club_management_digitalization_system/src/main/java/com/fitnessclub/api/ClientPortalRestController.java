package com.fitnessclub.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.service.ClientPortalService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientPortalRestController {

	private final ClientPortalService clientPortalService;

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
		return clientPortalService.membershipCatalog();
	}

	@GetMapping("/memberships/{membershipId}/rules")
	public List<Map<String, Object>> membershipRules(@PathVariable Integer membershipId) {
		return clientPortalService.membershipRules(membershipId);
	}

	@GetMapping("/services/catalog")
	public List<Map<String, Object>> serviceCatalog() {
		return clientPortalService.serviceCatalog();
	}

	@GetMapping("/services/{serviceId}/rules")
	public Map<String, Object> serviceRules(@PathVariable Integer serviceId) {
		return clientPortalService.serviceRulesOneRow(serviceId);
	}

	@PostMapping("/purchases/memberships")
	@ResponseStatus(HttpStatus.CREATED)
	public void buyMembership(@RequestBody Map<String, Object> body, HttpSession session) {
		clientPortalService.buyMembership(Integer.valueOf(String.valueOf(body.get("membership_id"))), session);
	}

	@PostMapping("/purchases/services")
	@ResponseStatus(HttpStatus.CREATED)
	public void buyService(@RequestBody Map<String, Object> body, HttpSession session) {
		clientPortalService.buyService(Integer.valueOf(String.valueOf(body.get("service_id"))), session);
	}

	@GetMapping("/purchases/history")
	public List<Map<String, Object>> purchaseHistory(HttpSession session) {
		return clientPortalService.purchaseHistory(session);
	}

	@GetMapping("/profile")
	public Map<String, Object> profile(HttpSession session) {
		return clientPortalService.profile(session);
	}

	@PutMapping("/profile")
	public void updateProfile(@RequestBody Map<String, Object> body, HttpSession session) {
		clientPortalService.updateProfile(body, session);
	}

	@PostMapping("/profile/documents")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Object> addDocument(@RequestBody Map<String, Object> body, HttpSession session) {
		return clientPortalService.addDocument(body, session);
	}

	@PutMapping("/profile/documents/{documentId}")
	public Map<String, Object> updateDocument(@PathVariable Integer documentId, @RequestBody Map<String, Object> body, HttpSession session) {
		return clientPortalService.updateDocument(documentId, body, session);
	}

	@DeleteMapping("/profile/documents/{documentId}")
	public void deleteDocument(@PathVariable Integer documentId, HttpSession session) {
		clientPortalService.deleteDocument(documentId, session);
	}

	@GetMapping("/notifications")
	public List<Map<String, Object>> notifications(HttpSession session) {
		return clientPortalService.notifications(session);
	}

	@GetMapping("/visitors")
	public Map<String, Object> visitors() {
		return clientPortalService.visitors();
	}

	@GetMapping("/training-plans")
	public List<Map<String, Object>> trainingPlans(HttpSession session) {
		return clientPortalService.trainingPlans(session);
	}

	@GetMapping("/memberships/overview")
	public Map<String, Object> membershipsOverview(HttpSession session) {
		return clientPortalService.membershipsOverview(session);
	}

	@GetMapping("/personal-trainings")
	public Map<String, Object> personalTrainingData(HttpSession session) {
		return clientPortalService.personalTrainingData(session);
	}

	@PostMapping("/personal-trainings/register")
	@ResponseStatus(HttpStatus.CREATED)
	public void registerPersonal(@RequestBody Map<String, Object> body, HttpSession session) {
		clientPortalService.registerPersonal(Integer.valueOf(String.valueOf(body.get("schedule_id"))), session);
	}

	@PostMapping("/personal-trainings/cancel")
	public void cancelPersonal(@RequestBody Map<String, Object> body, HttpSession session) {
		clientPortalService.cancelPersonal(Integer.valueOf(String.valueOf(body.get("purchase_id"))), session);
	}
}

