package com.fitnessclub.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.MembershipPurchase;

public interface MembershipPurchaseRepository extends JpaRepository<MembershipPurchase, Integer> {

	/**
	 * Все покупки клиента, действующие на дату (может быть несколько). Сначала более поздняя покупка (purchase_id).
	 * Нельзя использовать Optional — при 2+ строках Spring Data бросает IncorrectResultSizeDataAccessException.
	 */
	@Query("select p from MembershipPurchase p join fetch p.client join fetch p.membership where p.client.clientId = :clientId "
			+ "and lower(trim(p.status)) = lower(trim(:active)) and :today between p.startDate and p.endDate "
			+ "order by p.purchaseId desc")
	List<MembershipPurchase> findActiveForClientOnDate(@Param("clientId") Integer clientId,
			@Param("today") LocalDate today, @Param("active") String activeStatus);

	List<MembershipPurchase> findByClient_ClientId(Integer clientId);

	@Query("select p from MembershipPurchase p join fetch p.membership where p.client.clientId = :clientId order by p.purchaseDate desc")
	List<MembershipPurchase> findHistoryWithMembership(@Param("clientId") Integer clientId);

	@Query("select p from MembershipPurchase p join fetch p.membership where p.client.clientId = :clientId "
			+ "and lower(trim(p.status)) = 'активен' order by p.purchaseDate desc")
	List<MembershipPurchase> findCurrentManageableByClientId(@Param("clientId") Integer clientId);
}
