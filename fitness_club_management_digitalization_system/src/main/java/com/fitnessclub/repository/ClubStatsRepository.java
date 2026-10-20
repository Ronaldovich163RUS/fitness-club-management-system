package com.fitnessclub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessclub.domain.ClubStats;

public interface ClubStatsRepository extends JpaRepository<ClubStats, Integer> {

	Optional<ClubStats> findFirstByOrderByIdAsc();
}
