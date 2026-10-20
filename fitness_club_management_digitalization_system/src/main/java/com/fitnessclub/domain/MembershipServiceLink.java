package com.fitnessclub.domain;

import java.time.Instant;
import java.time.LocalTime;

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
@Table(name = "membership_service", uniqueConstraints = @UniqueConstraint(columnNames = { "membership_id", "service_id" }))
@Getter
@Setter
public class MembershipServiceLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "membership_service_id")
	private Integer membershipServiceId;

	@Column(name = "membership_id", nullable = false)
	private Integer membershipId;

	@Column(name = "service_id", nullable = false)
	private Integer serviceId;

	@Column(name = "included_visits")
	private Integer includedVisits;

	@Column(name = "max_per_week")
	private Integer maxPerWeek;

	@Column(name = "days_of_week")
	private String daysOfWeek;

	@Column(name = "time_start")
	private LocalTime timeStart;

	@Column(name = "time_end")
	private LocalTime timeEnd;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
