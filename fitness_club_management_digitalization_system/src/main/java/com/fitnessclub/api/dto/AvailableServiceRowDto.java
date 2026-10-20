package com.fitnessclub.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AvailableServiceRowDto {
	String source;
	Integer serviceId;
	String serviceName;
	String serviceType;
	Integer scheduleId;
	String details;
	Integer membershipPurchaseId;
	Integer servicePurchaseId;
	/** Можно нажать «Использовать» (окно времени, запись, места). */
	boolean usable;
}
