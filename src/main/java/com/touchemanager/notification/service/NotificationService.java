package com.touchemanager.notification.service;

import com.touchemanager.notification.dto.NotificationDTO;
import com.touchemanager.notification.dto.NotifyUpcomingBoutRequest;
import com.touchemanager.notification.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    /**
     * Notify both athletes of a bout that their bout starts in a few minutes.
     * Persists one notification per athlete and pushes it via SSE if the user is online.
     */
    List<NotificationDTO> notifyUpcomingBout(String requesterEmail, Long boutId, NotifyUpcomingBoutRequest request);

    /** Notification history of the authenticated user, newest first */
    List<NotificationDTO> getMyNotifications(String email);

    NotificationDTO markAsRead(String email, Long notificationId);

    void markAllAsRead(String email);

    /** Save and push a generic notification via SSE (if user is online); always persisted in DB */
    NotificationDTO sendNotification(Long recipientUserId, Long tournamentId, Long boutId, NotificationType type, String message);
}
