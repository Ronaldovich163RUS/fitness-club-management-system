package com.fitnessclub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fitnessclub.domain.Hall;

public interface HallRepository extends JpaRepository<Hall, Integer> {

	boolean existsByHallNameIgnoreCaseAndHallIdNot(String hallName, Integer hallId);

	boolean existsByHallNameIgnoreCase(String hallName);

	@Query("select h from Hall h left join fetch h.zone z")
	List<Hall> findAllFetchZone();

	List<Hall> findByIsActiveTrueOrderByHallNameAsc();
}
