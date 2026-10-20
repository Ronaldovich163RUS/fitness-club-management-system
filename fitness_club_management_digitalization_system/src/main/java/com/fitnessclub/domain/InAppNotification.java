package com.fitnessclub.domain;

import java.time.Instant;
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
@Table(name = "notification")
@Getter
@Setter
public class InAppNotification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Integer notificationId;

	@Column(name = "recipient_type", nullable = false, length = 16)
	private String recipientType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_client_id")
	private Client recipientClient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_trainer_id")
	private Trainer recipientTrainer;

	@Column(name = "sender_admin_id")
	private Integer senderAdminId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_trainer_id")
	private Trainer senderTrainer;

	@Column(name = "notification_type", nullable = false, length = 16)
	private String notificationType = "system";

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "is_sent", nullable = false)
	private Boolean isSent = Boolean.TRUE;

	@Column(name = "sent_datetime")
	private LocalDateTime sentDatetime;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;
}
