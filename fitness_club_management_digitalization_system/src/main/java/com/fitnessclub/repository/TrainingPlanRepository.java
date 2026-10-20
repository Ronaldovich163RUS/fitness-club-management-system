package com.fitnessclub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.TrainingPlanEntity;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlanEntity, Integer> {

	@Query("select p from TrainingPlanEntity p join fetch p.client c join fetch c.personalData join fetch p.trainer t join fetch t.personalData where p.trainer.trainerId = :trainerId order by p.updatedAt desc, p.planId desc")
	List<TrainingPlanEntity> findByTrainerIdFetchAll(@Param("trainerId") Integer trainerId);

	@Query("select p from TrainingPlanEntity p join fetch p.client c join fetch c.personalData join fetch p.trainer t join fetch t.personalData where p.planId = :planId")
	Optional<TrainingPlanEntity> findByIdFetchAll(@Param("planId") Integer planId);

	@Query("select p from TrainingPlanEntity p join fetch p.client c join fetch c.personalData join fetch p.trainer t join fetch t.personalData where c.clientId = :clientId order by p.updatedAt desc, p.planId desc")
	List<TrainingPlanEntity> findByClientIdFetchAll(@Param("clientId") Integer clientId);

	boolean existsByTrainer_TrainerIdAndClient_ClientId(Integer trainerId, Integer clientId);
}
