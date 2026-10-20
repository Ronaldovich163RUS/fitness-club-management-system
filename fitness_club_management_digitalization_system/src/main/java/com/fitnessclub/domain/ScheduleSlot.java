package com.fitnessclub.domain;

import java.time.LocalDateTime;

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
@Table(name = "schedule")
@Getter
@Setter
public class ScheduleSlot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "schedule_id")
	private Integer scheduleId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private FitnessServiceEntity service;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trainer_id", nullable = false)
	private Trainer trainer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hall_id", nullable = false)
	private Hall hall;

	@Column(name = "start_datetime", nullable = false)
	private LocalDateTime startDatetime;

	@Column(name = "end_datetime", nullable = false)
	private LocalDateTime endDatetime;

	@Column(name = "max_participants", nullable = false)
	private Integer maxParticipants;

	@Column(name = "current_participants", nullable = false)
	private Integer currentParticipants = 0;

	@Column(name = "is_cancelled")
	private Boolean isCancelled = Boolean.FALSE;
}
