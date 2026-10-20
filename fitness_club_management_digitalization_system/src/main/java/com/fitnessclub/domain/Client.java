package com.fitnessclub.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "client")
@Getter
@Setter
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "client_id")
	private Integer clientId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "personal_data_id", nullable = false, unique = true)
	private PersonalData personalData;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@Column(name = "has_student_id")
	private Boolean hasStudentId = Boolean.FALSE;

	@Column(name = "has_pension_cert")
	private Boolean hasPensionCert = Boolean.FALSE;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
