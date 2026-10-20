package com.fitnessclub.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fitnessclub.domain.Gender;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClientResponseDto {
	Integer clientId;
	Integer personalDataId;
	String lastName;
	String firstName;
	String middleName;
	LocalDate birthDate;
	String phone;
	String email;
	Gender gender;
	boolean isActive;
	String registrationAddress;
	String actualAddress;
}
