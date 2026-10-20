package com.fitnessclub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TrainerPagesController {

	@GetMapping("/trainer")
	public String trainerHome() {
		return "redirect:/trainer/profile";
	}

	@GetMapping("/trainer/profile")
	public String trainerProfile() {
		return "trainer/profile";
	}

	@GetMapping("/trainer/slots")
	public String trainerSlots() {
		return "trainer/slots";
	}

	@GetMapping("/trainer/plans")
	public String trainerPlans() {
		return "trainer/plans";
	}

	@GetMapping("/trainer/notifications")
	public String trainerNotifications() {
		return "trainer/notifications";
	}

	@GetMapping("/trainer/calendar")
	public String trainerCalendar() {
		return "trainer/calendar";
	}
}
