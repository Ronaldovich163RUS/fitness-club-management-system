package com.fitnessclub.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fitnessclub.domain.DocumentEntity;
import com.fitnessclub.domain.DocumentType;

/**
 * Набор проверенных документов, необходимый для статуса «активен» у тренера.
 */
public final class TrainerActiveDocumentRequirements {

	private TrainerActiveDocumentRequirements() {
	}

	public static List<String> missingVerifiedRequirements(List<DocumentEntity> docs) {
		Set<DocumentType> verified = docs.stream()
				.filter(d -> Boolean.TRUE.equals(d.getVerified()))
				.map(DocumentEntity::getDocumentType)
				.collect(Collectors.toSet());
		List<String> missing = new ArrayList<>();
		if (!verified.contains(DocumentType.PASSPORT_RF)) {
			missing.add("паспорт РФ (проверенный)");
		}
		if (!verified.contains(DocumentType.INN)) {
			missing.add("ИНН (проверенный)");
		}
		if (!verified.contains(DocumentType.SNILS)) {
			missing.add("СНИЛС (проверенный)");
		}
		if (!verified.contains(DocumentType.MEDICAL_BOOK_PAPER) && !verified.contains(DocumentType.MEDICAL_BOOK_ELECTRONIC)) {
			missing.add("медкнижка бумажная или электронная (проверенная)");
		}
		if (!verified.contains(DocumentType.DIPLOMA_HIGHER) && !verified.contains(DocumentType.DIPLOMA_SECONDARY)) {
			missing.add("диплом высшего или среднего проф. образования (проверенный)");
		}
		if (!verified.contains(DocumentType.WORK_RECORD_PAPER)) {
			missing.add("трудовая книжка (проверенная)");
		}
		return missing;
	}
}
