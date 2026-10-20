package com.fitnessclub.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VisitJournalRowDto {
	Integer visitId;
	String clientFullName;
	LocalDateTime entryDatetime;
	LocalDateTime exitDatetime;
	String visitType;
}
