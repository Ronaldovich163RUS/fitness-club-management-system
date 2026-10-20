package com.fitnessclub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessclub.domain.Zone;

public interface ZoneRepository extends JpaRepository<Zone, Integer> {

	boolean existsByZoneNameIgnoreCaseAndZoneIdNot(String zoneName, Integer zoneId);

	boolean existsByZoneNameIgnoreCase(String zoneName);

	Optional<Zone> findByZoneNameIgnoreCase(String zoneName);
}
