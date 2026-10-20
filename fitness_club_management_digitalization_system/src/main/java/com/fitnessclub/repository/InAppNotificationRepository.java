package com.fitnessclub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fitnessclub.domain.InAppNotification;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Integer> {

	@EntityGraph(attributePaths = { "recipientClient", "recipientClient.personalData", "recipientTrainer",
			"recipientTrainer.personalData" })
	List<InAppNotification> findAllByOrderBySentDatetimeDesc();

	@Query("select n from InAppNotification n join fetch n.recipientClient c join fetch c.personalData where lower(n.recipientType) = 'client' and n.senderTrainer.trainerId = :trainerId order by n.sentDatetime desc, n.notificationId desc")
	List<InAppNotification> findClientNotificationsSentByTrainer(@Param("trainerId") Integer trainerId);
}
