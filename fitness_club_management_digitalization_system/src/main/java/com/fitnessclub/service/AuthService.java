package com.fitnessclub.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.Gender;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.repository.AdminRepository;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.PersonalDataRepository;
import com.fitnessclub.repository.TrainerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	public enum Role {
		ADMIN, TRAINER, CLIENT
	}

	public record LoginResult(Role role, Integer profileId, boolean requiresRoleChoice, List<Role> availableRoles) {
	}

	private final PersonalDataRepository personalDataRepository;
	private final ClientRepository clientRepository;
	private final TrainerRepository trainerRepository;
	private final AdminRepository adminRepository;
	private final PasswordEncoder passwordEncoder;
	private final PersonalDataRulesService personalRules;

	private static final int MIN_AGE_CLIENT = 14;

	@Transactional(readOnly = true)
	public LoginResult login(String emailRaw, String passwordRaw) {
		String email = emailRaw == null ? "" : emailRaw.trim();
		String password = passwordRaw == null ? "" : passwordRaw;
		if (email.isBlank() || password.isBlank()) {
			throw new IllegalArgumentException("Введите email и пароль.");
		}
		PersonalData pd = personalDataRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль."));
		if (!passwordEncoder.matches(password, pd.getPasswordHash())) {
			throw new IllegalArgumentException("Неверный email или пароль.");
		}

		boolean hasAdmin = adminRepository.existsByPersonalDataId(pd.getPersonalDataId());
		boolean hasTrainer = trainerRepository.existsByPersonalData_PersonalDataId(pd.getPersonalDataId());
		boolean hasClient = clientRepository.existsByPersonalData_PersonalDataId(pd.getPersonalDataId());

		if (hasAdmin && !hasTrainer && !hasClient) {
			return new LoginResult(Role.ADMIN, pd.getPersonalDataId(), false, List.of(Role.ADMIN));
		}
		if (!hasAdmin && hasTrainer && !hasClient) {
			return new LoginResult(Role.TRAINER, pd.getPersonalDataId(), false, List.of(Role.TRAINER));
		}
		if (!hasAdmin && !hasTrainer && hasClient) {
			return new LoginResult(Role.CLIENT, pd.getPersonalDataId(), false, List.of(Role.CLIENT));
		}
		if (!hasAdmin && hasTrainer && hasClient) {
			List<Role> roles = new ArrayList<>();
			roles.add(Role.TRAINER);
			roles.add(Role.CLIENT);
			return new LoginResult(null, pd.getPersonalDataId(), true, roles);
		}
		throw new IllegalStateException("Некорректная конфигурация ролей пользователя.");
	}

	@Transactional(readOnly = true)
	public LoginResult chooseRole(Integer personalDataId, Role role) {
		if (personalDataId == null || role == null) {
			throw new IllegalArgumentException("Не выбрана роль.");
		}
		return switch (role) {
			case TRAINER -> {
				if (!trainerRepository.existsByPersonalData_PersonalDataId(personalDataId)) {
					throw new IllegalArgumentException("Роль тренера недоступна.");
				}
				yield new LoginResult(Role.TRAINER, personalDataId, false, List.of(Role.TRAINER));
			}
			case CLIENT -> {
				if (!clientRepository.existsByPersonalData_PersonalDataId(personalDataId)) {
					throw new IllegalArgumentException("Роль клиента недоступна.");
				}
				yield new LoginResult(Role.CLIENT, personalDataId, false, List.of(Role.CLIENT));
			}
			case ADMIN -> throw new IllegalArgumentException("Выбор роли администратора недоступен.");
		};
	}

	@Transactional
	public void registerClient(String emailRaw, String passwordRaw, String confirmPasswordRaw, String lastName, String firstName,
			String middleName, String phoneRaw, String birthDateRaw, String genderRaw) {
		Map<String, String> errors = new LinkedHashMap<>();
		LocalDate birthDate = parseBirthDateOrNull(birthDateRaw);
		Gender gender = parseGenderOrNull(genderRaw);
		personalRules.validatePersonNames(lastName, firstName, middleName, errors);
		personalRules.validateBirthDate(birthDate, MIN_AGE_CLIENT, errors, "birth_date");
		personalRules.validateGender(gender, errors);
		personalRules.validateEmailFormat(emailRaw, errors);
		personalRules.validatePhoneRaw(phoneRaw, errors);
		if (passwordRaw == null || confirmPasswordRaw == null || !passwordRaw.equals(confirmPasswordRaw)) {
			errors.put("password_confirm", "Пароль и подтверждение должны совпадать.");
		} else {
			TextNormalizationUtils.validatePasswordStrength(passwordRaw, errors, "password");
		}
		if (!errors.isEmpty()) {
			throw new ApiValidationException(errors);
		}

		String email = emailRaw.trim().toLowerCase(Locale.ROOT);
		String phoneNorm = personalRules.requireNormalizedPhone(phoneRaw);
		String phoneStore = "+" + phoneNorm;
		if (personalDataRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("Пользователь с таким email уже существует.");
		}
		if (personalDataRepository.existsByPhone(phoneStore)) {
			throw new IllegalArgumentException("Пользователь с таким телефоном уже существует.");
		}

		PersonalData pd = new PersonalData();
		pd.setEmail(email);
		pd.setPasswordHash(passwordEncoder.encode(passwordRaw));
		pd.setLastName(TextNormalizationUtils.capitalizeCyrillicNamePart(lastName.trim()));
		pd.setFirstName(TextNormalizationUtils.capitalizeCyrillicNamePart(firstName.trim()));
		pd.setMiddleName(middleName == null || middleName.isBlank() ? null
				: TextNormalizationUtils.capitalizeCyrillicNamePart(middleName.trim()));
		pd.setPhone(phoneStore);
		pd.setBirthDate(birthDate);
		pd.setGender(gender);
		pd.setRegistrationAddress("—");
		pd.setActualAddress(null);
		personalDataRepository.save(pd);

		Client c = new Client();
		c.setPersonalData(pd);
		c.setIsActive(Boolean.TRUE);
		clientRepository.save(c);
	}

	private static LocalDate parseBirthDateOrNull(String raw) {
		try {
			return LocalDate.parse(raw);
		} catch (Exception ex) {
			return null;
		}
	}

	private static Gender parseGenderOrNull(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Gender.valueOf(raw.trim());
		} catch (Exception ex) {
			return null;
		}
	}
}

