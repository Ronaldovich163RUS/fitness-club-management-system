package com.fitnessclub.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VisitEntryRequest {
	private Integer clientId;
	/** «обычный» или «гостевой» */
	private String visitType;
}
