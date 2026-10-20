package com.fitnessclub.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.domain.DocumentType;

@Service
public class DocumentRulesService {

	private static final Pattern DIGITS_4 = Pattern.compile("^\\d{4}$");
	private static final Pattern DIGITS_6 = Pattern.compile("^\\d{6}$");
	private static final Pattern DIGITS_12 = Pattern.compile("^\\d{12}$");
	private static final Pattern DIGITS_11 = Pattern.compile("^\\d{11}$");
	private static final Pattern MEDBOOK_NUM = Pattern.compile("^[A-Za-zА-Яа-яЁё0-9\\-]{8,16}$");
	private static final Pattern DIPLOMA_SERIES = Pattern.compile("^.{6}$");
	private static final Pattern DIPLOMA_NUM = Pattern.compile("^.{6,8}$");
	private static final Pattern WORK_NUM = Pattern.compile("^\\d{6,8}$");
	private static final Pattern STUDENT_NUM = Pattern.compile("^[A-Za-zА-Яа-яЁё0-9]{6,10}$");
	private static final Pattern PENSION_NUM = Pattern.compile("^\\d{6,12}$");

	public void validate(
			DocumentType type,
			String series,
			String number,
			String issuedBy,
			LocalDate issueDate,
			LocalDate expiryDate) {

		Map<String, String> errors = new LinkedHashMap<>();
		if (number == null || number.isBlank()) {
			errors.put("number", "Номер документа обязателен.");
		}
		LocalDate today = LocalDate.now();

		switch (type) {
			case PASSPORT_RF -> validatePassport(series, number, issuedBy, issueDate, expiryDate, today, errors);
			case INN -> validateInn(number, errors);
			case SNILS -> validateSnils(number, errors);
			case MEDICAL_BOOK_PAPER, MEDICAL_BOOK_ELECTRONIC -> validateMedical(
					series, number, issuedBy, issueDate, expiryDate, today, errors);
			case DIPLOMA_HIGHER, DIPLOMA_SECONDARY -> validateDiploma(series, number, issuedBy, issueDate, expiryDate, today, errors);
			case CERTIFICATE -> validateCertificate(number, issuedBy, issueDate, expiryDate, today, errors);
			case WORK_RECORD_PAPER -> validateWorkBook(series, number, issuedBy, issueDate, expiryDate, errors);
			case STUDENT_ID -> validateStudent(number, issuedBy, issueDate, expiryDate, today, errors);
			case PENSION_CERT -> validatePension(number, expiryDate, errors);
		}

		if (issueDate != null && expiryDate != null && issueDate.isAfter(expiryDate)) {
			errors.put("issue_date", "Дата выдачи не позже срока действия.");
		}
		if (issueDate != null && issueDate.isAfter(today)) {
			errors.put("issue_date", "Дата выдачи не в будущем.");
		}

		if (!errors.isEmpty()) {
			throw new ApiValidationException(errors);
		}
	}

	private void validatePassport(String series, String number, String issuedBy, LocalDate issueDate,
			LocalDate expiryDate, LocalDate today, Map<String, String> errors) {
		if (series == null || !DIGITS_4.matcher(series.trim()).matches()) {
			errors.put("series", "Серия паспорта РФ: 4 цифры.");
		}
		if (number == null || !DIGITS_6.matcher(number.trim()).matches()) {
			errors.put("number", "Номер паспорта РФ: 6 цифр.");
		}
		if (issuedBy == null || issuedBy.trim().length() < 5) {
			errors.put("issued_by", "Кем выдан: не менее 5 символов.");
		}
		if (issueDate == null) {
			errors.put("issue_date", "Дата выдачи обязательна.");
		} else if (issueDate.isAfter(today)) {
			errors.put("issue_date", "Дата выдачи не позднее сегодня.");
		}
		if (expiryDate != null && issueDate != null) {
			LocalDate minEnd = issueDate.plusYears(6);
			if (expiryDate.isBefore(minEnd)) {
				errors.put("expiry_date", "Срок действия паспорта должен быть не менее 6 лет от даты выдачи.");
			}
		}
	}

	private void validateInn(String number, Map<String, String> errors) {
		String d = number == null ? "" : number.replaceAll("\\D", "");
		if (!DIGITS_12.matcher(d).matches()) {
			errors.put("number", "ИНН физлица: 12 цифр.");
		}
	}

	private void validateSnils(String number, Map<String, String> errors) {
		String d = number == null ? "" : number.replaceAll("\\D", "");
		if (!DIGITS_11.matcher(d).matches()) {
			errors.put("number", "СНИЛС: 11 цифр после очистки.");
		}
	}

	private void validateMedical(String series, String number, String issuedBy, LocalDate issueDate, LocalDate expiryDate,
			LocalDate today, Map<String, String> errors) {
		String n = number == null ? "" : number.trim();
		if (!MEDBOOK_NUM.matcher(n).matches()) {
			errors.put("number", "Номер медкнижки: 8–16 символов (буквы, цифры, дефис).");
		}
		if (issuedBy == null || issuedBy.trim().length() < 2) {
			errors.put("issued_by", "Укажите орган, выдавший медкнижку.");
		}
		if (issueDate == null) {
			errors.put("issue_date", "Дата выдачи обязательна.");
		}
		if (expiryDate == null) {
			errors.put("expiry_date", "Срок действия медкнижки обязателен (до 1 года от выдачи).");
		} else if (issueDate != null) {
			if (!expiryDate.isAfter(issueDate)) {
				errors.put("expiry_date", "Срок действия позже даты выдачи.");
			}
			long days = ChronoUnit.DAYS.between(issueDate, expiryDate);
			if (days > 366) {
				errors.put("expiry_date", "Медкнижка не более чем на 1 год от даты выдачи.");
			}
		}
	}

	private void validateDiploma(String series, String number, String issuedBy, LocalDate issueDate, LocalDate expiryDate,
			LocalDate today, Map<String, String> errors) {
		if (series == null || !DIPLOMA_SERIES.matcher(series.trim()).matches()) {
			errors.put("series", "Серия диплома: 6 символов.");
		}
		if (number == null || !DIPLOMA_NUM.matcher(number.trim()).matches()) {
			errors.put("number", "Номер диплома: 6–8 символов.");
		}
		if (issuedBy == null || issuedBy.trim().length() < 5) {
			errors.put("issued_by", "Кем выдан: не менее 5 символов.");
		}
		if (issueDate == null) {
			errors.put("issue_date", "Дата выдачи обязательна.");
		}
	}

	private void validateCertificate(String number, String issuedBy, LocalDate issueDate, LocalDate expiryDate,
			LocalDate today, Map<String, String> errors) {
		String n = number == null ? "" : number.trim();
		if (n.length() < 1 || n.length() > 30) {
			errors.put("number", "Номер сертификата: 1–30 символов.");
		}
		if (issueDate == null) {
			errors.put("issue_date", "Дата выдачи обязательна.");
		}
		if (expiryDate != null && issueDate != null && !expiryDate.isAfter(issueDate)) {
			errors.put("expiry_date", "Срок действия позже даты выдачи.");
		}
	}

	private void validateWorkBook(String series, String number, String issuedBy, LocalDate issueDate, LocalDate expiryDate,
			Map<String, String> errors) {
		if (series == null || series.trim().length() < 1 || series.trim().length() > 20) {
			errors.put("series", "Серия трудовой книжки: укажите корректно (напр. ТК-V).");
		}
		if (number == null || !WORK_NUM.matcher(number.trim()).matches()) {
			errors.put("number", "Номер трудовой книжки: 6–8 цифр.");
		}
		if (issuedBy == null || issuedBy.trim().length() < 5) {
			errors.put("issued_by", "Кем выдан: не менее 5 символов.");
		}
	}

	private void validateStudent(String number, String issuedBy, LocalDate issueDate, LocalDate expiryDate,
			LocalDate today, Map<String, String> errors) {
		if (number == null || !STUDENT_NUM.matcher(number.trim()).matches()) {
			errors.put("number", "Номер студенческого: 6–10 символов.");
		}
		if (issueDate == null) {
			errors.put("issue_date", "Дата выдачи обязательна.");
		}
		if (expiryDate == null) {
			errors.put("expiry_date", "Срок действия студенческого обязателен.");
		} else if (issueDate != null) {
			LocalDate maxEnd = issueDate.plusYears(6);
			if (expiryDate.isAfter(maxEnd)) {
				errors.put("expiry_date", "Срок действия не более 6 лет от даты выдачи.");
			}
			if (!expiryDate.isAfter(issueDate)) {
				errors.put("expiry_date", "Срок действия позже даты выдачи.");
			}
		}
	}

	private void validatePension(String number, LocalDate expiryDate, Map<String, String> errors) {
		if (number == null || !PENSION_NUM.matcher(number.trim()).matches()) {
			errors.put("number", "Номер пенсионного: 6–12 цифр.");
		}
		if (expiryDate != null) {
			errors.put("expiry_date", "Для пенсионного удостоверения срок действия не указывается.");
		}
	}

	public String cleanSnils(String raw) {
		return raw == null ? "" : raw.replaceAll("\\D", "");
	}

	public String cleanInn(String raw) {
		return raw == null ? "" : raw.replaceAll("\\D", "");
	}
}
