package com.fitnessclub.service;

import org.springframework.stereotype.Component;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.repository.AdminRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentAdminIdResolver {

	private final AdminRepository adminRepository;

	public Integer requireDefaultAdminId() {
		return adminRepository.findFirstByOrderByAdminIdAsc()
				.map(a -> a.getAdminId())
				.orElseThrow(() -> ApiValidationException.single(
						"admin",
						"В базе нет учётной записи администратора. Перезапустите приложение для инициализации."));
	}
}
