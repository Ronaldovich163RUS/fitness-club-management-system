package com.fitnessclub.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fitnessclub.service.AuthService;
import com.fitnessclub.service.AuthService.LoginResult;
import com.fitnessclub.service.AuthService.Role;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public String login(@RequestParam("login") String login, @RequestParam("password") String password,
			HttpSession session) {
		try {
			LoginResult res = authService.login(login, password);
			session.removeAttribute("auth_pending_profile_id");
			session.removeAttribute("auth_pending_email");
			session.removeAttribute("auth_pending_roles");
			if (res.requiresRoleChoice()) {
				session.setAttribute("auth_pending_profile_id", res.profileId());
				session.setAttribute("auth_pending_email", login.trim().toLowerCase());
				session.setAttribute("auth_pending_roles", "TRAINER,CLIENT");
				return "redirect:/login?choose_role=1";
			}
			session.setAttribute("auth_email", login.trim().toLowerCase());
			session.setAttribute("auth_role", res.role().name());
			session.setAttribute("auth_profile_id", res.profileId());
			if (res.role() == Role.ADMIN) {
				return "redirect:/admin/clients";
			}
			if (res.role() == Role.TRAINER) {
				return "redirect:/trainer";
			}
			return "redirect:/client";
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return "redirect:/login?error=1";
		}
	}

	@PostMapping("/login/role")
	public String chooseRole(@RequestParam("role") String roleRaw, HttpSession session) {
		try {
			Object pendingProfile = session.getAttribute("auth_pending_profile_id");
			Object pendingEmail = session.getAttribute("auth_pending_email");
			Object pendingRoles = session.getAttribute("auth_pending_roles");
			if (pendingProfile == null || pendingEmail == null || pendingRoles == null) {
				return "redirect:/login?error=1";
			}
			Role role = Role.valueOf(roleRaw == null ? "" : roleRaw.trim().toUpperCase());
			String pendingRolesStr = String.valueOf(pendingRoles);
			if (!pendingRolesStr.contains(role.name())) {
				return "redirect:/login?error=1";
			}
			LoginResult res = authService.chooseRole(Integer.valueOf(String.valueOf(pendingProfile)), role);
			session.setAttribute("auth_email", String.valueOf(pendingEmail));
			session.setAttribute("auth_role", res.role().name());
			session.setAttribute("auth_profile_id", res.profileId());
			session.removeAttribute("auth_pending_profile_id");
			session.removeAttribute("auth_pending_email");
			session.removeAttribute("auth_pending_roles");
			if (res.role() == Role.TRAINER) {
				return "redirect:/trainer";
			}
			return "redirect:/client";
		} catch (Exception ex) {
			return "redirect:/login?error=1";
		}
	}

	@PostMapping("/register")
	public String register(@RequestParam("email") String email, @RequestParam("password") String password,
			@RequestParam("confirmPassword") String confirmPassword,
			@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName,
			@RequestParam(name = "middleName", required = false) String middleName,
			@RequestParam("phone") String phone, @RequestParam("birthDate") String birthDate,
			@RequestParam("gender") String gender) {
		try {
			authService.registerClient(email, password, confirmPassword, lastName, firstName, middleName, phone, birthDate, gender);
			return "redirect:/login?registered=1";
		} catch (IllegalArgumentException ex) {
			return "redirect:/register?error=1";
		} catch (Exception ex) {
			return "redirect:/register?error=1";
		}
	}
}

