package com.fitnessclub.domain;

import java.math.BigDecimal;
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
@Table(name = "service_purchase")
@Getter
@Setter
public class ServicePurchaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "purchase_id")
	private Integer purchaseId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private FitnessServiceEntity service;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id")
	private ScheduleSlot schedule;

	@Column(name = "is_used", nullable = false)
	private Boolean isUsed = Boolean.FALSE;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "price", precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "sold_by_admin_id")
	private Integer soldByAdminId;

	/**
	 * Дата/время покупки (как в БД / консоли MySQL), без пересчёта через UTC для отображения.
	 * См. {@code service_purchase.purchase_date}.
	 */
	@Generated(event = EventType.INSERT)
	@Column(name = "purchase_date", insertable = false, updatable = false)
	private LocalDateTime purchaseDate;
}
