package com.fitnessclub.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HallResponseDto {
	Integer hallId;
	String hallName;
	Integer zoneId;
	String zoneName;
	Integer capacity;
	String description;
	@JsonProperty("is_active")
	boolean isActive;
	Integer createdByAdminId;
}
