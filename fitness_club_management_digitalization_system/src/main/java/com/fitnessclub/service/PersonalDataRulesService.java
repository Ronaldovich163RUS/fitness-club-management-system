package com.fitnessclub.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.domain.Gender;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.repository.PersonalDataRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonalDataRulesService {

	private static final LocalDate MIN_BIRTH = LocalDate.of(1900, 1, 1);
	private final PersonalDataRepository personalDataRepository;

	public void validatePersonNames(String lastName, String firstName, String middleName, Map<String, String> errors) {
		String ln = TextNormalizationUtils.capitalizeCyrillicNamePart(lastName == null ? "" : lastName.trim());
		String fn = TextNormalizationUtils.capitalizeCyrillicNamePart(firstName == null ? "" : firstName.trim());
		if (!TextNormalizationUtils.isValidNamePart(ln)) {
			errors.put("last_name", "Фамилия: 2–50 символов, кириллица, дефис/пробел/апостроф между частями.");
		}
		if (!TextNormalizationUtils.isValidNamePart(fn)) {
			errors.put("first_name", "Имя: 2–50 символов, кириллица, дефис/пробел/апостроф между частями.");
		}
		if (middleName != null && !middleName.isBlank()) {
			String mn = TextNormalizationUtils.capitalizeCyrillicNamePart(middleName.trim());
			if (!TextNormalizationUtils.isValidNamePart(mn)) {
				errors.put("middle_name", "Отчество: 2–50 символов, кириллица, дефис/пробел/апостроф между частями.");
			}
		}
	}

	public void validateBirthDate(LocalDate birthDate, int minAgeYears, Map<String, String> errors, String field) {
		validateBirthDate(birthDate, minAgeYears, null, errors, field);
	}

	public void validateBirthDate(LocalDate birthDate, int minAgeYears, Integer maxAgeYears, Map<String, String> errors, String field) {
		if (birthDate == null) {
			errors.put(field, "Дата рождения обязательна.");
			return;
		}
		LocalDate today = LocalDate.now();
		if (birthDate.isAfter(today)) {
			errors.put(field, "Дата рождения не может быть в будущем.");
			return;
		}
		if (birthDate.isBefore(MIN_BIRTH)) {
			errors.put(field, "Дата рождения не ранее 01.01.1900.");
			return;
		}
		int age = Period.between(birthDate, today).getYears();
		if (age < minAgeYears) {
			errors.put(field, "Минимальный возраст: " + minAgeYears + " лет.");
			return;
		}
		if (maxAgeYears != null && age > maxAgeYears) {
			errors.put(field, "Максимальный возраст: " + maxAgeYears + " лет.");
		}
	}

	public void validateGender(Gender gender, Map<String, String> errors) {
		if (gender == null) {
			errors.put("gender", "Пол обязателен.");
		}
	}

	public void validateEmailFormat(String email, Map<String, String> errors) {
		if (email == null || email.isBlank()) {
			errors.put("email", "Email обязателен.");
			return;
		}
		String e = email.trim().toLowerCase();
		if (!TextNormalizationUtils.isValidEmail(e)) {
			errors.put("email", "Некорректный формат email.");
		}
	}

	public void validatePhoneRaw(String rawPhone, Map<String, String> errors) {
		validatePhoneRaw(rawPhone, errors, null);
	}

	public void validatePhoneRaw(String rawPhone, Map<String, String> errors, Integer excludePersonalDataId) {
		if (rawPhone == null || rawPhone.isBlank()) {
			errors.put("phone", "Телефон обязателен.");
			return;
		}
		String n = TextNormalizationUtils.normalizeRussianPhone(rawPhone);
		if (!TextNormalizationUtils.isValidNormalizedPhone(n)) {
			errors.put("phone", "Телефон: российский номер, после нормализации формат +7 и 10 цифр.");
			return;
		}
		// Если номер по факту не изменился у редактируемой записи, уникальность повторно не проверяем.
		if (excludePersonalDataId != null) {
			PersonalData existing = personalDataRepository.findById(excludePersonalDataId).orElse(null);
			if (existing != null && existing.getPhone() != null) {
				String existingNorm = TextNormalizationUtils.normalizeRussianPhone(existing.getPhone());
				if (TextNormalizationUtils.isValidNormalizedPhone(existingNorm) && existingNorm.equals(n)) {
					return;
				}
			}
		}
		String phoneStore = "+" + n;
		boolean taken = excludePersonalDataId == null
				? personalDataRepository.existsByPhone(phoneStore)
				: personalDataRepository.existsByPhoneAndPersonalDataIdNot(phoneStore, excludePersonalDataId);
		if (taken) {
			errors.put("phone", "Телефон уже занят.");
		}
	}

	public String requireNormalizedPhone(String rawPhone) {
		Map<String, String> err = new LinkedHashMap<>();
		validatePhoneRaw(rawPhone, err);
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
		return TextNormalizationUtils.normalizeRussianPhone(rawPhone);
	}

	public String normalizedPhoneOrThrow(String rawPhone) {
		return requireNormalizedPhone(rawPhone);
	}

	public String formatPhonePlus7(String normalized11) {
		return "+" + normalized11;
	}
}
