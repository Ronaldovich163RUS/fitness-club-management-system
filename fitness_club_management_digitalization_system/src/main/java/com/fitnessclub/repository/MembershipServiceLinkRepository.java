package com.fitnessclub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.MembershipServiceLink;

public interface MembershipServiceLinkRepository extends JpaRepository<MembershipServiceLink, Integer> {

	List<MembershipServiceLink> findByServiceId(Integer serviceId);

	List<MembershipServiceLink> findByMembershipIdOrderByServiceIdAsc(Integer membershipId);

	Optional<MembershipServiceLink> findByMembershipIdAndServiceId(Integer membershipId, Integer serviceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from MembershipServiceLink m where m.serviceId = :serviceId")
	void deleteByServiceId(@Param("serviceId") Integer serviceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from MembershipServiceLink m where m.membershipId = :membershipId")
	void deleteByMembershipId(@Param("membershipId") Integer membershipId);
}
