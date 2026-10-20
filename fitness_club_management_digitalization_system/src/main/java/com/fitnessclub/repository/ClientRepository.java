package com.fitnessclub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.Client;
import com.fitnessclub.domain.Trainer;

public interface ClientRepository extends JpaRepository<Client, Integer> {

	@Query("select c from Client c join fetch c.personalData")
	List<Client> findAllWithPersonalData();

	@Query("select c from Client c join fetch c.personalData where c.clientId = :id")
	Optional<Client> findByIdFetchPersonal(@Param("id") Integer id);

	@Query("select c from Client c join fetch c.personalData p where c.personalData.personalDataId not in "
			+ "(select t.personalData.personalDataId from Trainer t) order by p.lastName, p.firstName")
	List<Client> findNotTrainerWithPersonalOrderByName();

	boolean existsByPersonalData_PersonalDataId(Integer personalDataId);
	Optional<Client> findByPersonalData_PersonalDataId(Integer personalDataId);

	Optional<Client> findFirstByIsActiveTrueOrderByClientIdAsc();
}
