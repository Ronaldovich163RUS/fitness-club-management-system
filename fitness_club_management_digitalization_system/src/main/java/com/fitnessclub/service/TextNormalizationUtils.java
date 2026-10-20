package com.fitnessclub.service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class TextNormalizationUtils {

	private static final Pattern NAME_PART = Pattern.compile(
			"^[А-ЯЁ][а-яё]*(?:[ '\\-–—][А-ЯЁ][а-яё]*)*$");
	private static final Pattern EMAIL = Pattern.compile(
			"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
	private static final Pattern NORMALIZED_PHONE = Pattern.compile("^7\\d{10}$");
	private static final Pattern ADDRESS_BAD = Pattern.compile(".*[<>{}].*", Pattern.DOTALL);
	private static final Pattern PASSWORD_COMPLEX = Pattern.compile(
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

	private TextNormalizationUtils() {
	}

	public static boolean isValidEmail(String email) {
		return email != null && EMAIL.matcher(email).matches();
	}

	public static boolean isValidNormalizedPhone(String phone) {
		return phone != null && NORMALIZED_PHONE.matcher(phone).matches();
	}

	public static String normalizeRussianPhone(String raw) {
		if (raw == null) {
			return null;
		}
		String digits = raw.replaceAll("\\D", "");
		if (digits.length() == 11 && digits.startsWith("8")) {
			digits = "7" + digits.substring(1);
		}
		if (digits.length() == 10) {
			digits = "7" + digits;
		}
		return digits;
	}

	public static boolean isValidNamePart(String part) {
		return part != null && part.length() >= 2 && part.length() <= 50 && NAME_PART.matcher(part).matches();
	}

	public static String capitalizeCyrillicNamePart(String input) {
		if (input == null || input.isBlank()) {
			return input;
		}
		Locale ru = Locale.forLanguageTag("ru");
		StringBuilder out = new StringBuilder();
		boolean newWord = true;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == ' ' || c == '-' || c == '–' || c == '—') {
				out.append(c);
				newWord = true;
				continue;
			}
			if (c == '\'' || c == '’') {
				out.append(c);
				newWord = true;
				continue;
			}
			if (newWord) {
				out.append(Character.toUpperCase(c));
				newWord = false;
			} else {
				out.append(Character.toLowerCase(c));
			}
		}
		return out.toString();
	}

	public static void validateAddress(String value, String field, boolean required, Map<String, String> errors) {
		if (value == null || value.isBlank()) {
			if (required) {
				errors.put(field, "Адрес обязателен (минимум 10 символов).");
			}
			return;
		}
		if (value.length() < 10) {
			errors.put(field, "Минимальная длина адреса — 10 символов.");
		}
		if (ADDRESS_BAD.matcher(value).matches()) {
			errors.put(field, "Недопустимые символы: < > { }");
		}
	}

	public static void validatePasswordStrength(String password, Map<String, String> errors, String field) {
		if (password == null || !PASSWORD_COMPLEX.matcher(password).matches()) {
			errors.put(field,
					"Пароль от 8 символов: буквы верхнего и нижнего регистра, цифры и спецсимвол.");
		}
	}
}
