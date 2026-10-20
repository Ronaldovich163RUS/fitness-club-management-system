package com.fitnessclub.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ServiceUpsertRequest {

	private String serviceName;
	private String serviceType;
	private Integer durationMinutes;
	private String description;
	private BigDecimal basePrice;
	private Integer zoneId;
	private Boolean isActive;
	private List<Integer> membershipIds;
}
