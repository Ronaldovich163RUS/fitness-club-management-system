package com.fitnessclub.api.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MembershipServiceRuleResponseDto {
	Integer membershipServiceId;
	Integer membershipId;
	String membershipName;
	Integer serviceId;
	String serviceName;
	Integer includedVisits;
	Integer maxPerWeek;
	List<String> daysOfWeek;
	String timeStart;
	String timeEnd;
}
