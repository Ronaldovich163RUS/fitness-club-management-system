package com.fitnessclub.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fitnessclub.service.AuthService.Role;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SessionRoleInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String path = request.getRequestURI();
		Role required = requiredRole(path);
		if (required == null) {
			return true;
		}
		Object roleObj = request.getSession().getAttribute("auth_role");
		if (roleObj == null) {
			response.sendRedirect("/login");
			return false;
		}
		Role actual;
		try {
			actual = Role.valueOf(String.valueOf(roleObj));
		} catch (Exception ex) {
			response.sendRedirect("/login");
			return false;
		}
		if (actual != required) {
			response.sendRedirect(defaultPage(actual));
			return false;
		}
		return true;
	}

	private static Role requiredRole(String path) {
		if (path.startsWith("/admin") || path.startsWith("/api/admin")) {
			return Role.ADMIN;
		}
		if (path.startsWith("/trainer") || path.startsWith("/api/trainer")) {
			return Role.TRAINER;
		}
		if (path.startsWith("/client") || path.startsWith("/api/client")) {
			return Role.CLIENT;
		}
		return null;
	}

	private static String defaultPage(Role r) {
		if (r == Role.ADMIN) return "/admin/clients";
		if (r == Role.TRAINER) return "/trainer";
		return "/client";
	}
}

