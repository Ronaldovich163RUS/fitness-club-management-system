package com.fitnessclub.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ScheduleUpsertRequest {

	private Integer serviceId;
	private Integer trainerId;
	private Integer hallId;
	private LocalDateTime startDatetime;
	private LocalDateTime endDatetime;
	private Integer maxParticipants;
	private Integer currentParticipants;
	private Boolean isCancelled;
}
