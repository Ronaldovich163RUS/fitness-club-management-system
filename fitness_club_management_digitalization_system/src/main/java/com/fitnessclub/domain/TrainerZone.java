package com.fitnessclub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trainer_zone", uniqueConstraints = @UniqueConstraint(columnNames = { "trainer_id", "zone_id" }))
@Getter
@Setter
public class TrainerZone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "trainer_zone_id")
	private Integer trainerZoneId;

	@Column(name = "trainer_id", nullable = false)
	private Integer trainerId;

	@Column(name = "zone_id", nullable = false)
	private Integer zoneId;
}
