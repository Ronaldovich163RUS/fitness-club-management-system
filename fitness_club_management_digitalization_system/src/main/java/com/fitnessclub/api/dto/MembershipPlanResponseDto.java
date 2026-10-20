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
public class MembershipPlanResponseDto {
	Integer membershipId;
	String membershipName;
	String description;
	BigDecimal price;
	Integer durationDays;
	Integer visitsLimit;
	Integer freezeDays;
	Integer guestVisits;
	boolean parking;
	boolean towel;
	@JsonProperty("is_active")
	boolean isActive;
	List<Integer> serviceIds;
}
