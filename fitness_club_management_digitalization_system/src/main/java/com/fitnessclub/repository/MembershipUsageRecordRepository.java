package com.fitnessclub.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.MembershipUsageRecord;

public interface MembershipUsageRecordRepository extends JpaRepository<MembershipUsageRecord, Integer> {

	long countByMembershipPurchase_PurchaseIdAndService_ServiceId(Integer purchaseId, Integer serviceId);

	@Query("select count(u) from MembershipUsageRecord u where u.membershipPurchase.purchaseId = :pid "
			+ "and u.service.serviceId = :sid and u.usedAt >= :since")
	long countForPurchaseServiceSince(@Param("pid") Integer purchaseId, @Param("sid") Integer serviceId,
			@Param("since") LocalDateTime since);

	@Query("""
			select count(u) > 0 from MembershipUsageRecord u
			join u.membershipPurchase mp
			where mp.client.clientId = :clientId
			  and u.service.serviceId = :serviceId
			  and u.usedAt >= :since
			""")
	boolean existsForClientServiceSince(@Param("clientId") Integer clientId, @Param("serviceId") Integer serviceId,
			@Param("since") LocalDateTime since);

	@Query("""
			select distinct u.service.serviceId from MembershipUsageRecord u
			join u.membershipPurchase mp
			where mp.client.clientId = :clientId and u.usedAt >= :since
			""")
	List<Integer> findDistinctServiceIdsForClientSince(@Param("clientId") Integer clientId,
			@Param("since") LocalDateTime since);
}
