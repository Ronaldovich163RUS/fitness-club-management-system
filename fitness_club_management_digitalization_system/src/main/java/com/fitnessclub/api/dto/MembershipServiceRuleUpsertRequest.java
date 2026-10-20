package com.fitnessclub.api.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MembershipServiceRuleUpsertRequest {

	/** Для создания / поиска пары */
	private Integer membershipId;
	private Integer serviceId;

	private Integer includedVisits;
	private Integer maxPerWeek;
	private List<String> daysOfWeek;
	/** «HH:mm» или «HH:mm:ss» */
	private String timeStart;
	private String timeEnd;
}
