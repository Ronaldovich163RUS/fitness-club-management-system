package com.fitnessclub.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessclub.domain.PersonalData;

public interface PersonalDataRepository extends JpaRepository<PersonalData, Integer> {

	Optional<PersonalData> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByPhone(String phone);

	boolean existsByEmailIgnoreCaseAndPersonalDataIdNot(String email, Integer personalDataId);

	boolean existsByPhoneAndPersonalDataIdNot(String phone, Integer personalDataId);
}
