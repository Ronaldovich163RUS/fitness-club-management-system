package com.fitnessclub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicPagesController {

	@GetMapping("/")
	public String home() {
		return "redirect:/schedule";
	}

	@GetMapping("/schedule")
	public String schedule() {
		return "schedule";
	}

	@GetMapping("/trainings")
	public String trainings() {
		return "trainings";
	}

	@GetMapping("/offers")
	public String offers() {
		return "offers";
	}

	@GetMapping("/trainers")
	public String trainers() {
		return "trainers";
	}

	@GetMapping("/faq")
	public String faq() {
		return "faq";
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String register() {
		return "registration";
	}

}

