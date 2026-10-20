package com.fitnessclub.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hall")
@Getter
@Setter
public class Hall {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "hall_id")
	private Integer hallId;

	@Column(name = "hall_name", nullable = false, length = 100)
	private String hallName;

	@Column(name = "capacity", nullable = false)
	private Integer capacity;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "zone_id")
	private Zone zone;

	@Column(name = "created_by_admin_id", nullable = false)
	private Integer createdByAdminId;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
