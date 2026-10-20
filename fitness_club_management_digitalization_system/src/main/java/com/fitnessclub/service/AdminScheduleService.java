package com.fitnessclub.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import com.fitnessclub.ClubZone;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnessclub.api.ApiValidationException;
import com.fitnessclub.api.dto.ScheduleResponseDto;
import com.fitnessclub.api.dto.ScheduleUpsertRequest;
import com.fitnessclub.domain.FitnessServiceEntity;
import com.fitnessclub.domain.Hall;
import com.fitnessclub.domain.ScheduleSlot;
import com.fitnessclub.domain.Trainer;
import com.fitnessclub.repository.FitnessServiceRepository;
import com.fitnessclub.repository.HallRepository;
import com.fitnessclub.repository.ScheduleSlotRepository;
import com.fitnessclub.repository.TrainerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminScheduleService {

	private static final int MAX_PART_MIN = 1;
	private static final int MAX_PART_MAX = 500;
	private static final LocalTime SCHEDULE_EARLIEST_START = LocalTime.of(6, 0);
	private static final LocalTime SCHEDULE_LATEST_START = LocalTime.of(22, 0);
	private static final int MAX_SESSION_DURATION_MINUTES = 240;
	/** Минимальный перерыв между окончанием одного занятия и началом следующего (тот же зал или тот же тренер). */
	private static final int MIN_GAP_BETWEEN_SLOTS_MINUTES = 10;
	private static final String GROUP_SERVICE_TYPE = "групповая тренировка";

	private final ScheduleSlotRepository scheduleSlotRepository;
	private final FitnessServiceRepository serviceRepository;
	private final TrainerRepository trainerRepository;
	private final HallRepository hallRepository;

	@Transactional(readOnly = true)
	public List<ScheduleResponseDto> listAll() {
		LocalDateTime from = LocalDateTime.now(ClubZone.ZONE);
		return scheduleSlotRepository.findFutureGroupSessionsFetchRelations(from).stream()
				.map(this::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public ScheduleResponseDto create(ScheduleUpsertRequest req) {
		validate(req, null, true);
		ScheduleSlot s = new ScheduleSlot();
		apply(s, req, true);
		scheduleSlotRepository.save(s);
		return toDto(scheduleSlotRepository.findById(s.getScheduleId()).orElse(s));
	}

	@Transactional
	public ScheduleResponseDto update(Integer scheduleId, ScheduleUpsertRequest req) {
		ScheduleSlot s = scheduleSlotRepository.findById(scheduleId)
				.orElseThrow(() -> ApiValidationException.single("schedule_id", "Запись расписания не найдена."));
		ensureGroupScheduleSlot(s);
		validate(req, scheduleId, false);
		apply(s, req, false);
		return toDto(s);
	}

	@Transactional
	public void delete(Integer scheduleId) {
		ScheduleSlot s = scheduleSlotRepository.findById(scheduleId)
				.orElseThrow(() -> ApiValidationException.single("schedule_id", "Запись расписания не найдена."));
		ensureGroupScheduleSlot(s);
		scheduleSlotRepository.deleteById(scheduleId);
	}

	private void ensureGroupScheduleSlot(ScheduleSlot s) {
		String st = s.getService().getServiceType() == null ? "" : s.getService().getServiceType().trim();
		if (!GROUP_SERVICE_TYPE.equalsIgnoreCase(st)) {
			throw ApiValidationException.single("schedule_id", "Управление доступно только для групповых занятий.");
		}
	}

	private void validate(ScheduleUpsertRequest req, Integer excludeScheduleId, boolean creating) {
		Map<String, String> err = new LinkedHashMap<>();
		if (creating && Boolean.TRUE.equals(req.getIsCancelled())) {
			err.put("is_cancelled", "Новое занятие нельзя создавать со статусом «Отменено».");
		}
		LocalDateTime start = req.getStartDatetime();
		LocalDateTime end = req.getEndDatetime();
		if (start == null) {
			err.put("start_datetime", "Укажите дату и время начала.");
		}
		if (end == null) {
			err.put("end_datetime", "Укажите дату и время окончания.");
		}
		if (start != null && end != null && !end.isAfter(start)) {
			err.put("end_datetime", "Окончание должно быть позже начала.");
		}
		if (start != null && start.isBefore(LocalDateTime.now())) {
			err.put("start_datetime", "Нельзя назначать занятие в прошлом.");
		}
		if (start != null) {
			LocalTime t0 = start.toLocalTime();
			if (t0.isBefore(SCHEDULE_EARLIEST_START) || t0.isAfter(SCHEDULE_LATEST_START)) {
				err.put("start_datetime", "Время начала: с 06:00 до 22:00.");
			}
		}
		if (start != null && end != null) {
			long durMin = ChronoUnit.MINUTES.between(start, end);
			if (durMin > MAX_SESSION_DURATION_MINUTES) {
				err.put("end_datetime", "Длительность занятия не более " + MAX_SESSION_DURATION_MINUTES + " мин.");
			}
		}
		Integer maxP = req.getMaxParticipants();
		if (maxP == null || maxP < MAX_PART_MIN || maxP > MAX_PART_MAX) {
			err.put("max_participants", "Максимум участников: от " + MAX_PART_MIN + " до " + MAX_PART_MAX + ".");
		}
		Integer cur = req.getCurrentParticipants();
		if (cur != null && cur < 0) {
			err.put("current_participants", "Число записанных не может быть отрицательным.");
		}
		if (maxP != null && cur != null && cur > maxP) {
			err.put("current_participants", "Записанных не больше максимума участников.");
		}
		Integer svcId = req.getServiceId();
		if (svcId == null || !serviceRepository.existsById(svcId)) {
			err.put("service_id", "Укажите существующую услугу.");
		} else {
			serviceRepository.findById(svcId).ifPresent(svc -> {
				if (!Boolean.TRUE.equals(svc.getIsActive())) {
					err.put("service_id", "Услуга неактивна.");
				}
				String st = svc.getServiceType() == null ? "" : svc.getServiceType().trim();
				if (!GROUP_SERVICE_TYPE.equalsIgnoreCase(st)) {
					err.put("service_id", "В расписание добавляются только групповые тренировки.");
				}
			});
		}
		Integer tid = req.getTrainerId();
		if (tid == null) {
			err.put("trainer_id", "Выберите тренера.");
		} else {
			var trOpt = trainerRepository.findByIdFetchPersonal(tid);
			if (trOpt.isEmpty()) {
				err.put("trainer_id", "Тренер не найден.");
			} else if (!Boolean.TRUE.equals(trOpt.get().getIsActive())) {
				err.put("trainer_id", "Нельзя назначить неактивного тренера на занятие.");
			}
		}
		Integer hid = req.getHallId();
		if (hid == null) {
			err.put("hall_id", "Выберите зал.");
		} else {
			var hOpt = hallRepository.findById(hid);
			if (hOpt.isEmpty()) {
				err.put("hall_id", "Зал не найден.");
			} else {
				Hall h = hOpt.get();
				if (Boolean.FALSE.equals(h.getIsActive())) {
					err.put("hall_id", "Зал неактивен.");
				}
				if (maxP != null && h.getCapacity() != null && maxP > h.getCapacity()) {
					err.put("max_participants", "Не больше вместимости зала (" + h.getCapacity() + " чел.).");
				}
			}
		}
		if (!err.isEmpty()) {
			throw new ApiValidationException(err);
		}
		boolean proposedActive = !Boolean.TRUE.equals(req.getIsCancelled());
		if (proposedActive && start != null && end != null && hid != null && tid != null) {
			LocalDateTime rangeStart = start.minusMinutes(MIN_GAP_BETWEEN_SLOTS_MINUTES);
			LocalDateTime rangeEnd = end.plusMinutes(MIN_GAP_BETWEEN_SLOTS_MINUTES);
			long hallClash = excludeScheduleId == null
					? scheduleSlotRepository.countActiveTooCloseInHall(hid, rangeStart, rangeEnd)
					: scheduleSlotRepository.countActiveTooCloseInHallExcluding(hid, rangeStart, rangeEnd, excludeScheduleId);
			if (hallClash > 0) {
				throw ApiValidationException.single("hall_id",
						"В зале следующее занятие может начаться не ранее чем через " + MIN_GAP_BETWEEN_SLOTS_MINUTES
								+ " мин после окончания предыдущего.");
			}
			long trClash = excludeScheduleId == null
					? scheduleSlotRepository.countActiveTooCloseForTrainer(tid, rangeStart, rangeEnd)
					: scheduleSlotRepository.countActiveTooCloseForTrainerExcluding(tid, rangeStart, rangeEnd, excludeScheduleId);
			if (trClash > 0) {
				throw ApiValidationException.single("trainer_id",
						"Между двумя занятиями одного тренера должно пройти не менее " + MIN_GAP_BETWEEN_SLOTS_MINUTES + " мин.");
			}
		}
	}

	private void apply(ScheduleSlot s, ScheduleUpsertRequest req, boolean isCreate) {
		FitnessServiceEntity svc = serviceRepository.findById(req.getServiceId()).orElseThrow();
		Trainer tr = trainerRepository.findByIdFetchPersonal(req.getTrainerId()).orElseThrow();
		Hall hall = hallRepository.findById(req.getHallId()).orElseThrow();
		s.setService(svc);
		s.setTrainer(tr);
		s.setHall(hall);
		s.setStartDatetime(req.getStartDatetime());
		s.setEndDatetime(req.getEndDatetime());
		s.setMaxParticipants(req.getMaxParticipants());
		if (isCreate) {
			s.setCurrentParticipants(req.getCurrentParticipants() != null ? req.getCurrentParticipants() : 0);
		} else if (req.getCurrentParticipants() != null) {
			s.setCurrentParticipants(req.getCurrentParticipants());
		}
		s.setIsCancelled(Boolean.TRUE.equals(req.getIsCancelled()) ? Boolean.TRUE : Boolean.FALSE);
	}

	private ScheduleResponseDto toDto(ScheduleSlot s) {
		var p = s.getTrainer().getPersonalData();
		String fullName = String.join(" ", p.getLastName(), p.getFirstName(),
				p.getMiddleName() == null || p.getMiddleName().isBlank() ? "" : p.getMiddleName()).trim();
		return ScheduleResponseDto.builder()
				.scheduleId(s.getScheduleId())
				.serviceId(s.getService().getServiceId())
				.serviceName(s.getService().getServiceName())
				.serviceType(s.getService().getServiceType())
				.trainerId(s.getTrainer().getTrainerId())
				.trainerFullName(fullName)
				.hallId(s.getHall().getHallId())
				.hallName(s.getHall().getHallName())
				.startDatetime(s.getStartDatetime())
				.endDatetime(s.getEndDatetime())
				.maxParticipants(s.getMaxParticipants())
				.currentParticipants(s.getCurrentParticipants())
				.isCancelled(Boolean.TRUE.equals(s.getIsCancelled()))
				.build();
	}
}
