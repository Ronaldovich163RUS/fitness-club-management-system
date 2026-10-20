package com.fitnessclub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.Trainer;

public interface TrainerRepository extends JpaRepository<Trainer, Integer> {

	@Query("select t from Trainer t join fetch t.personalData")
	List<Trainer> findAllWithPersonalData();

	@Query("select t from Trainer t join fetch t.personalData where t.trainerId = :id")
	Optional<Trainer> findByIdFetchPersonal(@Param("id") Integer id);

	@Query("select t from Trainer t join fetch t.personalData p where t.isActive = true order by p.lastName, p.firstName")
	List<Trainer> findActiveWithPersonalOrderByName();

	@Query("select t from Trainer t join fetch t.personalData p where t.personalData.personalDataId not in "
			+ "(select c.personalData.personalDataId from Client c) order by p.lastName, p.firstName")
	List<Trainer> findNotClientWithPersonalOrderByName();

	boolean existsByPersonalData_PersonalDataId(Integer personalDataId);
}
