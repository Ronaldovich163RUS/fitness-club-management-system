package com.fitnessclub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientPagesController {

	@GetMapping("/client")
	public String clientHome() {
		return "redirect:/client/profile";
	}

	@GetMapping("/client/profile")
	public String clientProfile() {
		return "client/profile";
	}

	@GetMapping("/client/schedule")
	public String clientSchedule() {
		return "client/schedule";
	}

	@GetMapping("/client/training-types")
	public String clientTrainingTypes() {
		return "client/training-types";
	}

	@GetMapping("/client/personal-training")
	public String clientPersonalTraining() {
		return "client/personal-training";
	}

	@GetMapping("/client/training-plans")
	public String clientTrainingPlans() {
		return "client/training-plans";
	}

	@GetMapping("/client/trainers")
	public String clientTrainers() {
		return "client/trainers";
	}

	@GetMapping("/client/memberships")
	public String clientMemberships() {
		return "client/memberships";
	}

	@GetMapping("/client/buy-memberships")
	public String clientBuyMemberships() {
		return "client/buy-memberships";
	}

	@GetMapping("/client/buy-services")
	public String clientBuyServices() {
		return "client/buy-services";
	}

	@GetMapping("/client/purchase-history")
	public String clientPurchaseHistory() {
		return "redirect:/client/memberships";
	}

	@GetMapping("/client/purchases")
	public String clientPurchasesLegacy() {
		return "redirect:/client/buy-memberships";
	}

	@GetMapping("/client/notifications")
	public String clientNotifications() {
		return "client/notifications";
	}

	@GetMapping("/client/visitors")
	public String clientVisitors() {
		return "client/visitors";
	}

	@GetMapping("/client/faq")
	public String clientFaq() {
		return "client/faq";
	}
}
