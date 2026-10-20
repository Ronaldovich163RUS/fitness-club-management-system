package com.fitnessclub.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.ServicePurchaseEntity;

public interface ServicePurchaseEntityRepository extends JpaRepository<ServicePurchaseEntity, Integer> {

	boolean existsByService_ServiceId(Integer serviceId);

	@EntityGraph(attributePaths = { "service", "schedule", "schedule.trainer" })
	List<ServicePurchaseEntity> findByClient_ClientIdAndIsUsedFalseOrderByPurchaseIdAsc(Integer clientId);

	@EntityGraph(attributePaths = { "service", "schedule", "schedule.trainer" })
	Optional<ServicePurchaseEntity> findFirstByClient_ClientIdAndSchedule_ScheduleIdAndIsUsedFalse(Integer clientId,
			Integer scheduleId);

	@EntityGraph(attributePaths = { "client", "service", "schedule", "schedule.trainer" })
	@Query("select p from ServicePurchaseEntity p where p.purchaseId = :purchaseId")
	Optional<ServicePurchaseEntity> findByIdWithDetails(@Param("purchaseId") Integer purchaseId);

	@Query("select p from ServicePurchaseEntity p join fetch p.service where p.client.clientId = :clientId order by p.purchaseDate desc")
	List<ServicePurchaseEntity> findHistoryWithService(@Param("clientId") Integer clientId);

	@Query("select distinct p.client.clientId from ServicePurchaseEntity p where p.schedule.trainer.trainerId = :trainerId")
	List<Integer> findDistinctClientIdsByTrainerSchedule(@Param("trainerId") Integer trainerId);

	@Query("select distinct p.client.clientId from ServicePurchaseEntity p where p.schedule is not null and p.schedule.trainer.trainerId = :trainerId and lower(trim(p.service.serviceType)) = 'персональная тренировка'")
	List<Integer> findDistinctClientIdsByTrainerPersonalSchedule(@Param("trainerId") Integer trainerId);

	@EntityGraph(attributePaths = { "client", "service", "schedule", "schedule.trainer" })
	@Query("select p from ServicePurchaseEntity p where p.schedule is not null and p.schedule.trainer.trainerId = :trainerId")
	List<ServicePurchaseEntity> findByTrainerSchedule(@Param("trainerId") Integer trainerId);

	@Query("select p from ServicePurchaseEntity p where p.schedule.scheduleId = :scheduleId")
	List<ServicePurchaseEntity> findBySchedule_ScheduleId(@Param("scheduleId") Integer scheduleId);

	@EntityGraph(attributePaths = { "client", "client.personalData" })
	@Query("select p from ServicePurchaseEntity p where p.schedule.scheduleId = :scheduleId")
	List<ServicePurchaseEntity> findWithClientByScheduleId(@Param("scheduleId") Integer scheduleId);

	@Query("""
			select count(p) > 0 from ServicePurchaseEntity p
			where p.client.clientId = :clientId
			  and p.schedule.scheduleId = :scheduleId
			  and p.isUsed = true
			""")
	boolean existsUsedByClient_ClientIdAndSchedule_ScheduleId(@Param("clientId") Integer clientId,
			@Param("scheduleId") Integer scheduleId);

	@Query("""
			select count(p) > 0 from ServicePurchaseEntity p
			where p.client.clientId = :clientId
			  and p.service.serviceId = :serviceId
			  and p.isUsed = true
			  and p.usedAt is not null
			  and p.usedAt >= :since
			""")
	boolean existsUsedForClientServiceSince(@Param("clientId") Integer clientId,
			@Param("serviceId") Integer serviceId, @Param("since") LocalDateTime since);

	@Query("""
			select distinct p.service.serviceId from ServicePurchaseEntity p
			where p.client.clientId = :clientId
			  and p.isUsed = true
			  and p.usedAt is not null
			  and p.usedAt >= :since
			""")
	List<Integer> findDistinctServiceIdsUsedByClientSince(@Param("clientId") Integer clientId,
			@Param("since") LocalDateTime since);
}
