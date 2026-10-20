package com.fitnessclub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fitnessclub.web.SessionRoleInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

	private final SessionRoleInterceptor sessionRoleInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(sessionRoleInterceptor)
				.addPathPatterns("/admin/**", "/trainer/**", "/client/**", "/api/admin/**", "/api/trainer/**", "/api/client/**")
				.excludePathPatterns("/login", "/register", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico");
	}
}

