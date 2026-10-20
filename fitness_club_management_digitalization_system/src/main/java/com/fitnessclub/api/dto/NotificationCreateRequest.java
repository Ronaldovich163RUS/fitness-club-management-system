package com.fitnessclub.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificationCreateRequest {
	private String recipientType;
	/** null — всем клиентам или всем тренерам */
	private Integer recipientId;
	private String title;
	private String message;
}
