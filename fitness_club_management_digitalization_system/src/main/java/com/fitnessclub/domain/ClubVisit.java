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
@Table(name = "visit")
@Getter
@Setter
public class ClubVisit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "visit_id")
	private Integer visitId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Column(name = "entry_datetime", nullable = false)
	private LocalDateTime entryDatetime;

	@Column(name = "exit_datetime")
	private LocalDateTime exitDatetime;

	@Column(name = "visit_type", nullable = false, length = 32)
	private String visitType;

	@Column(name = "registered_by_admin_id", nullable = false)
	private Integer registeredByAdminId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id")
	private ScheduleSlot schedule;

	/**
	 * Устаревшее поле в БД (осталось после ddl-auto). Логика визита его не использует —
	 * фиксации услуг учитываются по {@code membership_usage} и {@code service_purchase.used_at}.
	 */
	@Column(name = "service_fixed", nullable = false)
	private Boolean serviceFixed = Boolean.FALSE;
}
