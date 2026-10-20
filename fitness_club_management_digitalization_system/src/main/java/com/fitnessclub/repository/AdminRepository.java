package com.fitnessclub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessclub.domain.AdminEntity;

public interface AdminRepository extends JpaRepository<AdminEntity, Integer> {

	Optional<AdminEntity> findFirstByOrderByAdminIdAsc();

	boolean existsByPersonalDataId(Integer personalDataId);
}
