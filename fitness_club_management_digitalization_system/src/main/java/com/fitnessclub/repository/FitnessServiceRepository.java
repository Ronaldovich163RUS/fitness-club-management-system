package com.fitnessclub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.FitnessServiceEntity;

public interface FitnessServiceRepository extends JpaRepository<FitnessServiceEntity, Integer> {

	boolean existsByServiceNameIgnoreCaseAndServiceIdNot(String serviceName, Integer serviceId);

	boolean existsByServiceNameIgnoreCase(String serviceName);

	@Query("select s from FitnessServiceEntity s left join fetch s.zone z")
	List<FitnessServiceEntity> findAllFetchZone();

	@Query("select count(s) > 0 from FitnessServiceEntity s where lower(trim(s.serviceType)) = 'персональная тренировка' and s.zone.zoneId = :zoneId")
	boolean existsPersonalByZoneId(@Param("zoneId") Integer zoneId);

	@Query("select count(s) > 0 from FitnessServiceEntity s where lower(trim(s.serviceType)) = 'персональная тренировка' and s.zone.zoneId = :zoneId and s.serviceId <> :excludeServiceId")
	boolean existsPersonalByZoneIdExcluding(@Param("zoneId") Integer zoneId, @Param("excludeServiceId") Integer excludeServiceId);

	@Query("select s from FitnessServiceEntity s left join fetch s.zone z where lower(trim(s.serviceType)) = 'персональная тренировка' and s.isActive = true and s.zone.zoneId = :zoneId")
	List<FitnessServiceEntity> findActivePersonalByZoneId(@Param("zoneId") Integer zoneId);
}
