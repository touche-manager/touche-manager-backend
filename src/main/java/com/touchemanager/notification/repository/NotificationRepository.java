package com.touchemanager.notification.repository;

import com.touchemanager.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientUserId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId);
}
