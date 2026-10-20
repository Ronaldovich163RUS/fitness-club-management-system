package com.fitnessclub.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.service.TrainerPortalService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trainer")
@RequiredArgsConstructor
public class TrainerPortalRestController {

	private final TrainerPortalService trainerPortalService;

	@GetMapping("/profile")
	public Map<String, Object> profile(HttpSession session) {
		return trainerPortalService.profile(session);
	}

	@PutMapping("/profile")
	public void updateProfile(@RequestBody Map<String, Object> body, HttpSession session) {
		trainerPortalService.updateProfile(body, session);
	}

	@GetMapping("/slots/meta")
	public Map<String, Object> slotsMeta(HttpSession session) {
		return trainerPortalService.slotsMeta(session);
	}

	@GetMapping("/slots")
	public List<Map<String, Object>> slots(
			@RequestParam(name = "personalOnly", defaultValue = "false") boolean personalOnly,
			HttpSession session) {
		return trainerPortalService.slots(session, personalOnly);
	}

	@PostMapping("/slots")
	@ResponseStatus(HttpStatus.CREATED)
	public void createSlot(@RequestBody Map<String, Object> body, HttpSession session) {
		trainerPortalService.createSlot(body, session);
	}

	@PostMapping("/slots/{scheduleId}/cancel")
	public void cancelSlot(@PathVariable Integer scheduleId, HttpSession session) {
		trainerPortalService.cancelSlot(scheduleId, session);
	}

	@GetMapping("/clients")
	public List<Map<String, Object>> assignedClients(HttpSession session) {
		return trainerPortalService.assignedClients(session);
	}

	@GetMapping("/plans")
	public List<Map<String, Object>> plans(HttpSession session) {
		return trainerPortalService.plans(session);
	}

	@PostMapping("/plans")
	@ResponseStatus(HttpStatus.CREATED)
	public void createPlan(@RequestBody Map<String, Object> body, HttpSession session) {
		trainerPortalService.createPlan(body, session);
	}

	@PutMapping("/plans/{planId}")
	public void updatePlan(@PathVariable Integer planId, @RequestBody Map<String, Object> body, HttpSession session) {
		trainerPortalService.updatePlan(planId, body, session);
	}

	@DeleteMapping("/plans/{planId}")
	public void deletePlan(@PathVariable Integer planId, HttpSession session) {
		trainerPortalService.deletePlan(planId, session);
	}

	@PostMapping("/notifications")
	@ResponseStatus(HttpStatus.CREATED)
	public void sendNotification(@RequestBody Map<String, Object> body, HttpSession session) {
		trainerPortalService.sendNotification(body, session);
	}
}
