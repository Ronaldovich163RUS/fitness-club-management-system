package com.fitnessclub.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.ScheduleSlot;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Integer> {

	@Query("select s from ScheduleSlot s join fetch s.service join fetch s.trainer t join fetch t.personalData join fetch s.hall h left join fetch h.zone")
	List<ScheduleSlot> findAllFetchRelations();

	/** Групповые занятия, которые ещё не завершились к моменту {@code from} (страница админ-расписания). */
	@Query("""
			select s from ScheduleSlot s
			join fetch s.service svc
			join fetch s.trainer t
			join fetch t.personalData
			join fetch s.hall h
			left join fetch h.zone
			where lower(trim(svc.serviceType)) = 'групповая тренировка'
			  and s.endDatetime >= :from
			order by s.startDatetime asc
			""")
	List<ScheduleSlot> findFutureGroupSessionsFetchRelations(@Param("from") LocalDateTime from);

	@Query("select s from ScheduleSlot s join fetch s.service join fetch s.trainer t join fetch t.personalData join fetch s.hall h left join fetch h.zone where t.trainerId = :trainerId order by s.startDatetime asc")
	List<ScheduleSlot> findByTrainerIdFetchRelations(@Param("trainerId") Integer trainerId);

	@Query("""
			select s from ScheduleSlot s
			join fetch s.service svc
			join fetch s.trainer t
			join fetch t.personalData
			join fetch s.hall h
			left join fetch h.zone
			where t.trainerId = :trainerId
			  and lower(trim(svc.serviceType)) = 'персональная тренировка'
			order by s.startDatetime asc
			""")
	List<ScheduleSlot> findPersonalByTrainerIdFetchRelations(@Param("trainerId") Integer trainerId);

	@Query("select s from ScheduleSlot s join fetch s.service join fetch s.trainer t join fetch t.personalData join fetch s.hall h left join fetch h.zone where s.scheduleId = :scheduleId")
	Optional<ScheduleSlot> findByIdFetchRelations(@Param("scheduleId") Integer scheduleId);

	boolean existsByService_ServiceId(Integer serviceId);
	boolean existsByTrainer_TrainerId(Integer trainerId);

	boolean existsByTrainer_TrainerIdAndStartDatetimeGreaterThanEqual(Integer trainerId, LocalDateTime from);

	/** Групповые занятия услуги: ещё не закончились, начало не позже {@code latestStart}. */
	@Query("""
			select s from ScheduleSlot s
			join fetch s.service svc
			join fetch s.trainer t
			join fetch t.personalData
			join fetch s.hall h
			left join fetch h.zone
			where svc.serviceId = :serviceId
			  and (s.isCancelled is null or s.isCancelled = false)
			  and s.endDatetime >= :now
			  and s.startDatetime <= :latestStart
			order by s.startDatetime asc
			""")
	List<ScheduleSlot> findSlotsForServiceInUseWindow(@Param("serviceId") Integer serviceId,
			@Param("now") LocalDateTime now, @Param("latestStart") LocalDateTime latestStart);

	/** Пересечение с «окном» [rangeStart, rangeEnd]: перекрытие, смежность или интервал меньше зазора. */
	@Query("SELECT COUNT(s) FROM ScheduleSlot s WHERE s.hall.hallId = :hallId AND (s.isCancelled IS NULL OR s.isCancelled = false) AND s.startDatetime < :rangeEnd AND s.endDatetime > :rangeStart")
	long countActiveTooCloseInHall(@Param("hallId") Integer hallId, @Param("rangeStart") LocalDateTime rangeStart,
			@Param("rangeEnd") LocalDateTime rangeEnd);

	@Query("SELECT COUNT(s) FROM ScheduleSlot s WHERE s.hall.hallId = :hallId AND (s.isCancelled IS NULL OR s.isCancelled = false) AND s.scheduleId <> :excludeId AND s.startDatetime < :rangeEnd AND s.endDatetime > :rangeStart")
	long countActiveTooCloseInHallExcluding(@Param("hallId") Integer hallId,
			@Param("rangeStart") LocalDateTime rangeStart, @Param("rangeEnd") LocalDateTime rangeEnd,
			@Param("excludeId") Integer excludeId);

	@Query("SELECT COUNT(s) FROM ScheduleSlot s WHERE s.trainer.trainerId = :trainerId AND (s.isCancelled IS NULL OR s.isCancelled = false) AND s.startDatetime < :rangeEnd AND s.endDatetime > :rangeStart")
	long countActiveTooCloseForTrainer(@Param("trainerId") Integer trainerId,
			@Param("rangeStart") LocalDateTime rangeStart, @Param("rangeEnd") LocalDateTime rangeEnd);

	@Query("SELECT COUNT(s) FROM ScheduleSlot s WHERE s.trainer.trainerId = :trainerId AND (s.isCancelled IS NULL OR s.isCancelled = false) AND s.scheduleId <> :excludeId AND s.startDatetime < :rangeEnd AND s.endDatetime > :rangeStart")
	long countActiveTooCloseForTrainerExcluding(@Param("trainerId") Integer trainerId,
			@Param("rangeStart") LocalDateTime rangeStart, @Param("rangeEnd") LocalDateTime rangeEnd,
			@Param("excludeId") Integer excludeId);
}
