package com.fitnessclub.web;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fitnessclub.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminPagesController {

	private final ZoneRepository zoneRepository;

	@GetMapping("/admin")
	public String adminHome() {
		return "redirect:/admin/clients";
	}

	@GetMapping("/admin/clients")
	public String adminClients() {
		return "admin/clients";
	}

	@GetMapping("/admin/trainers")
	public String adminTrainers(Model model) {
		model.addAttribute("zones", zoneRepository.findAll(Sort.by("zoneName")));
		return "admin/trainers";
	}

	@GetMapping("/admin/memberships")
	public String adminMemberships() {
		return "admin/memberships";
	}

	@GetMapping("/admin/membership-rules")
	public String adminMembershipRules() {
		return "admin/membership-rules";
	}

	@GetMapping("/admin/training-categories")
	public String adminTrainingCategories() {
		return "admin/training-categories";
	}

	@GetMapping("/admin/zones-halls")
	public String adminZonesHallsRedirect() {
		return "redirect:/admin/zones";
	}

	@GetMapping("/admin/zones")
	public String adminZones() {
		return "admin/zones";
	}

	@GetMapping("/admin/halls")
	public String adminHalls() {
		return "admin/halls";
	}

	@GetMapping("/admin/schedule")
	public String adminSchedule() {
		return "admin/schedule";
	}

	@GetMapping("/admin/notifications")
	public String adminNotifications() {
		return "admin/notifications";
	}

	@GetMapping("/admin/visits")
	public String adminVisits() {
		return "admin/visits";
	}
}

