package com.fitnessclub.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UseServiceRequest {
	private Integer clientId;
	/** «абонемент» или «разовая» */
	private String source;
	private Integer membershipPurchaseId;
	private Integer servicePurchaseId;
	private Integer serviceId;
	private Integer scheduleId;
}
