package com.fitnessclub.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.NotificationCreateRequest;
import com.fitnessclub.api.dto.NotificationResponseDto;
import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.InAppNotification;
import com.fitnessclub.domain.PersonalData;
import com.fitnessclub.domain.Trainer;
import com.fitnessclub.repository.ClientRepository;
import com.fitnessclub.repository.InAppNotificationRepository;
import com.fitnessclub.repository.TrainerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

	private static final String TYPE_SYSTEM = "system";

	private final InAppNotificationRepository notificationRepository;
	private final ClientRepository clientRepository;
	private final TrainerRepository trainerRepository;
	private final CurrentAdminIdResolver adminIdResolver;

	@Transactional(readOnly = true)
	public List<NotificationResponseDto> listAll() {
		return notificationRepository.findAllByOrderBySentDatetimeDesc().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public void create(NotificationCreateRequest req) {
		Map<String, String> err = new LinkedHashMap<>();
		String rt = req.getRecipientType() == null ? "" : req.getRecipientType().trim().toLowerCase();
		if (!"client".equals(rt) && !"trainer".equals(rt)) {
			err.put("recipient_type", "Укажите роль: client или trainer.");
		}
		String title = req.getTitle() == null ? "" : req.getTitle().trim();
		String message = req.getMessage() == null ? "" : req.getMessage().trim();
		if (title.isEmpty()) {
			err.put("title", "Укажите заголовок.");
		} else if (title.length() > 200) {
			err.put("title", "Заголовок не длиннее 200 символов.");
		}
		if (message.isEmpty()) {
			err.put("message", "Укажите текст сообщения.");
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
		Integer adminId = adminIdResolver.requireDefaultAdminId();
		LocalDateTime now = LocalDateTime.now();
		if ("trainer".equals(rt)) {
			if (req.getRecipientId() != null) {
				Trainer t = trainerRepository.findByIdFetchPersonal(req.getRecipientId())
						.orElseThrow(() -> ApiValidationException.single("recipient_id", "Тренер не найден."));
				saveNotification(adminId, now, rt, null, t, title, message);
			} else {
				for (Trainer t : trainerRepository.findAllWithPersonalData()) {
					saveNotification(adminId, now, rt, null, t, title, message);
				}
			}
		} else {
			if (req.getRecipientId() != null) {
				Client c = clientRepository.findByIdFetchPersonal(req.getRecipientId())
						.orElseThrow(() -> ApiValidationException.single("recipient_id", "Клиент не найден."));
				saveNotification(adminId, now, rt, c, null, title, message);
			} else {
				for (Client c : clientRepository.findAllWithPersonalData()) {
					saveNotification(adminId, now, rt, c, null, title, message);
				}
			}
		}
	}

	private void saveNotification(Integer adminId, LocalDateTime now, String recipientType, Client client,
			Trainer trainer, String title, String message) {
		InAppNotification n = new InAppNotification();
		n.setRecipientType(recipientType);
		n.setRecipientClient(client);
		n.setRecipientTrainer(trainer);
		n.setSenderAdminId(adminId);
		n.setNotificationType(TYPE_SYSTEM);
		n.setTitle(title);
		n.setMessage(message);
		n.setIsSent(Boolean.TRUE);
		n.setSentDatetime(now);
		notificationRepository.save(n);
	}

	private NotificationResponseDto toDto(InAppNotification n) {
		String label;
		if ("trainer".equalsIgnoreCase(n.getRecipientType())) {
			if (n.getRecipientTrainer() != null) {
				label = fullName(n.getRecipientTrainer().getPersonalData());
			} else {
				label = "—";
			}
		} else {
			if (n.getRecipientClient() != null) {
				label = fullName(n.getRecipientClient().getPersonalData());
			} else {
				label = "—";
			}
		}
		return NotificationResponseDto.builder()
				.notificationId(n.getNotificationId())
				.recipientType(n.getRecipientType())
				.recipientLabel(label)
				.title(n.getTitle())
				.message(n.getMessage())
				.isSent(Boolean.TRUE.equals(n.getIsSent()))
				.sentDatetime(n.getSentDatetime())
				.build();
	}

	private String fullName(PersonalData p) {
		String mn = p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : " " + p.getMiddleName();
		return (p.getLastName() + " " + p.getFirstName() + mn).trim();
	}
}
