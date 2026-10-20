package com.fitnessclub.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ScheduleResponseDto {
	Integer scheduleId;
	Integer serviceId;
	String serviceName;
	String serviceType;
	Integer trainerId;
	String trainerFullName;
	Integer hallId;
	String hallName;
	LocalDateTime startDatetime;
	LocalDateTime endDatetime;
	Integer maxParticipants;
	Integer currentParticipants;
	boolean isCancelled;
}
