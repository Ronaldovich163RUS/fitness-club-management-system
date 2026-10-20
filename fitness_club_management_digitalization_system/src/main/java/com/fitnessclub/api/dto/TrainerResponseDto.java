package com.fitnessclub.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fitnessclub.domain.Gender;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainerResponseDto {
	Integer trainerId;
	Integer personalDataId;
	String lastName;
	String firstName;
	String middleName;
	LocalDate birthDate;
	String phone;
	String email;
	Gender gender;
	String specialization;
	Integer experienceYears;
	BigDecimal rating;
	@JsonProperty("is_active")
	boolean isActive;
	String biography;
	List<Integer> zoneIds;
	List<String> zoneNames;
	String registrationAddress;
	String actualAddress;
}
