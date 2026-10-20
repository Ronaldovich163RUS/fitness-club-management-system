package com.fitnessclub.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fitnessclub.api.dto.NotificationCreateRequest;
import com.fitnessclub.api.dto.NotificationResponseDto;
import com.fitnessclub.service.AdminNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationRestController {

	private final AdminNotificationService notificationService;

	@GetMapping
	public List<NotificationResponseDto> list() {
		return notificationService.listAll();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@RequestBody NotificationCreateRequest body) {
		notificationService.create(body);
	}
}
