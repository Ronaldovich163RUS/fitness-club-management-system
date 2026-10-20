package com.fitnessclub.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "personal_data")
@Getter
@Setter
public class PersonalData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "personal_data_id")
	private Integer personalDataId;

	@Column(name = "last_name", nullable = false, length = 50)
	private String lastName;

	@Column(name = "first_name", nullable = false, length = 50)
	private String firstName;

	@Column(name = "middle_name", length = 50)
	private String middleName;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(name = "phone", nullable = false, length = 20, unique = true)
	private String phone;

	@Column(name = "email", nullable = false, unique = true)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", nullable = false, length = 16)
	private Gender gender;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "registration_address", columnDefinition = "TEXT")
	private String registrationAddress;

	@Column(name = "actual_address", columnDefinition = "TEXT")
	private String actualAddress;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;
}
