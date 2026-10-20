package com.fitnessclub.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trainer")
@Getter
@Setter
public class Trainer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "trainer_id")
	private Integer trainerId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "personal_data_id", nullable = false, unique = true)
	private PersonalData personalData;

	@Column(name = "specialization", nullable = false, length = 100)
	private String specialization;

	@Column(name = "experience_years", nullable = false)
	private Integer experienceYears;

	@Column(name = "rating", precision = 3, scale = 2)
	private BigDecimal rating;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@Column(name = "biography", columnDefinition = "TEXT")
	private String biography;

	@Column(name = "photo_url", length = 255)
	private String photoUrl;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
