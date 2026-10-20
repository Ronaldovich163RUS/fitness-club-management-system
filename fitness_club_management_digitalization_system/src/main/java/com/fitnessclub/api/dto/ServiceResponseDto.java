package com.fitnessclub.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ServiceResponseDto {
	Integer serviceId;
	String serviceName;
	String serviceType;
	Integer durationMinutes;
	String description;
	BigDecimal basePrice;
	Integer zoneId;
	String zoneName;
	@JsonProperty("is_active")
	boolean isActive;
	List<Integer> membershipIds;
}
