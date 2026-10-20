package com.fitnessclub.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin")
@Getter
@Setter
public class AdminEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "admin_id")
	private Integer adminId;

	@Column(name = "personal_data_id", nullable = false, unique = true)
	private Integer personalDataId;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
