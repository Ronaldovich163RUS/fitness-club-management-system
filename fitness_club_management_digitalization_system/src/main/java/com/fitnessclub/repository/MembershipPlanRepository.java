package com.fitnessclub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fitnessclub.domain.MembershipPlan;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Integer> {

	List<MembershipPlan> findAllByOrderByMembershipNameAsc();
	List<MembershipPlan> findByIsActiveTrueOrderByMembershipNameAsc();

	boolean existsByMembershipNameIgnoreCase(String membershipName);

	boolean existsByMembershipNameIgnoreCaseAndMembershipIdNot(String membershipName, Integer membershipId);

	Optional<MembershipPlan> findFirstByMembershipNameIgnoreCase(String membershipName);
}
