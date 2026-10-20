package com.fitnessclub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessclub.domain.DocumentEntity;
import com.fitnessclub.domain.DocumentType;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Integer> {

	List<DocumentEntity> findByPersonalDataPersonalDataIdOrderByDocumentId(Integer personalDataId);

	boolean existsByPersonalDataPersonalDataIdAndDocumentTypeAndNumber(
			Integer personalDataId, DocumentType type, String number);

	boolean existsByPersonalDataPersonalDataIdAndDocumentTypeAndNumberAndDocumentIdNot(
			Integer personalDataId, DocumentType type, String number, Integer documentId);

	Optional<DocumentEntity> findByDocumentIdAndPersonalDataPersonalDataId(Integer documentId, Integer personalDataId);

	boolean existsByPersonalDataPersonalDataIdAndDocumentTypeAndVerifiedTrue(Integer personalDataId, DocumentType documentType);
}
