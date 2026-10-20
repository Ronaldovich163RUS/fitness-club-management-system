package com.fitnessclub.domain;

import java.math.BigDecimal;
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
@Table(name = "membership")
@Getter
@Setter
public class MembershipPlan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "membership_id")
	private Integer membershipId;

	@Column(name = "membership_name", nullable = false, length = 50)
	private String membershipName;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "price", nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "duration_days", nullable = false)
	private Integer durationDays;

	@Column(name = "visits_limit")
	private Integer visitsLimit;

	@Column(name = "freeze_days")
	private Integer freezeDays;

	@Column(name = "guest_visits")
	private Integer guestVisits;

	@Column(name = "parking")
	private Boolean parking;

	@Column(name = "towel")
	private Boolean towel;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@Column(name = "created_by_admin_id", nullable = false)
	private Integer createdByAdminId;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
