package com.fitnessclub.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MembershipPlanUpsertRequest {

	private String membershipName;
	private String description;
	private BigDecimal price;
	private Integer durationDays;
	private Integer visitsLimit;
	private Integer freezeDays;
	private Integer guestVisits;
	private Boolean parking;
	private Boolean towel;
	private Boolean isActive;
	private List<Integer> serviceIds;
}
