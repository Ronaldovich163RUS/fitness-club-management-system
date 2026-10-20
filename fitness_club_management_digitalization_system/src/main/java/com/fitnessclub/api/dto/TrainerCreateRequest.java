package com.fitnessclub.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fitnessclub.domain.Gender;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TrainerCreateRequest {

	private String lastName;
	private String firstName;
	private String middleName;
	private LocalDate birthDate;
	private String phone;
	private String email;
	private Gender gender;
	private String registrationAddress;
	private String actualAddress;
	private String password;
	private String passwordConfirm;

	private String specialization;
	private Integer experienceYears;
	private BigDecimal rating;
	private String biography;
	private Boolean isActive;
	private List<Integer> zoneIds;
}
