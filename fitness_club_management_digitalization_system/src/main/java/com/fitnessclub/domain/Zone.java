package com.fitnessclub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "zone")
@Getter
@Setter
public class Zone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "zone_id")
	private Integer zoneId;

	@Column(name = "zone_name", nullable = false, unique = true, length = 50)
	private String zoneName;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;
}
