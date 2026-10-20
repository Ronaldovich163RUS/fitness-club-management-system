package com.fitnessclub.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fitnessclub.domain.DocumentType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DocumentResponseDto {
	Integer documentId;
	Integer personalDataId;
	DocumentType documentType;
	String series;
	String number;
	String issuedBy;
	LocalDate issueDate;
	LocalDate expiryDate;
	String notes;
	/** Явное имя JSON: иначе Jackson даёт `verified`, а фронт ждёт `is_verified`. */
	@JsonProperty("is_verified")
	boolean verified;
}
