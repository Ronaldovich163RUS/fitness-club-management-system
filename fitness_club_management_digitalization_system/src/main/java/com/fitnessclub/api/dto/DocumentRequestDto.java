package com.fitnessclub.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fitnessclub.domain.DocumentType;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DocumentRequestDto {

	private DocumentType documentType;
	private String series;
	private String number;
	private String issuedBy;
	private LocalDate issueDate;
	private LocalDate expiryDate;
	private String notes;

	@JsonProperty("is_verified")
	private Boolean verified;
}
