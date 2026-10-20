package com.fitnessclub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.TrainerZone;

public interface TrainerZoneRepository extends JpaRepository<TrainerZone, Integer> {

	List<TrainerZone> findByTrainerId(Integer trainerId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from TrainerZone tz where tz.trainerId = :trainerId")
	void deleteByTrainerId(@Param("trainerId") Integer trainerId);
}
