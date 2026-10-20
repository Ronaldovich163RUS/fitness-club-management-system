package com.fitnessclub.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HallUpsertRequest {

	private String hallName;
	private Integer zoneId;
	private Integer capacity;
	private String description;
	private Boolean isActive;
}
