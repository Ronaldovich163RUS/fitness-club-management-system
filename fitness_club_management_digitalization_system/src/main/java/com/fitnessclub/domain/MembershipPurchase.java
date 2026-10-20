package com.fitnessclub.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "membership_purchase")
@Getter
@Setter
public class MembershipPurchase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "purchase_id")
	private Integer purchaseId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "membership_id", nullable = false)
	private MembershipPlan membership;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "status", nullable = false, length = 32)
	private String status;

	@Column(name = "remaining_guest_visits")
	private Integer remainingGuestVisits;

	@Column(name = "remaining_freeze_days")
	private Integer remainingFreezeDays;

	@Column(name = "freeze_started_on")
	private LocalDate freezeStartedOn;

	@Column(name = "freeze_requested_days")
	private Integer freezeRequestedDays;

	@Column(name = "price", precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "sold_by_admin_id")
	private Integer soldByAdminId;

	/**
	 * Дата/время покупки (как в БД / консоли MySQL), без пересчёта через UTC для отображения.
	 * См. {@code membership_purchase.purchase_date}.
	 */
	@Generated(event = EventType.INSERT)
	@Column(name = "purchase_date", insertable = false, updatable = false)
	private LocalDateTime purchaseDate;
}
