package com.fitnessclub.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fitnessclub.domain.Gender;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClientCreateRequest {

	private String lastName;
	private String firstName;
	private String middleName;
	private LocalDate birthDate;
	private String phone;
	private String email;
	private Gender gender;
	private Boolean isActive;
	private String registrationAddress;
	private String actualAddress;
	private String password;
	private String passwordConfirm;
}
