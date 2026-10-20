package com.fitnessclub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fitnessclub.domain.ClubVisit;

public interface ClubVisitRepository extends JpaRepository<ClubVisit, Integer> {

	boolean existsByClient_ClientIdAndExitDatetimeIsNull(Integer clientId);

	List<ClubVisit> findByClient_ClientIdAndExitDatetimeIsNullOrderByEntryDatetimeAsc(Integer clientId);

	List<ClubVisit> findByExitDatetimeIsNullOrderByEntryDatetimeDesc();

	@Query("select v from ClubVisit v join fetch v.client c join fetch c.personalData where v.exitDatetime is null order by v.entryDatetime desc")
	List<ClubVisit> findOpenVisitsFetchClient();

	@Query("select v from ClubVisit v join fetch v.client c join fetch c.personalData order by v.entryDatetime desc")
	List<ClubVisit> findAllFetchClientOrderByEntryDesc();

	boolean existsByClient_ClientIdAndSchedule_ScheduleId(Integer clientId, Integer scheduleId);
}
