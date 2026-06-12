package com.touchemanager.notification.dto;

import com.touchemanager.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        Long tournamentId,
        Long boutId,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}
