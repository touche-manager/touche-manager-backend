package com.touchemanager.notification.controller;

import com.touchemanager.notification.dto.NotificationDTO;
import com.touchemanager.notification.dto.NotifyUpcomingBoutRequest;
import com.touchemanager.notification.service.NotificationService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/api/bouts/{boutId}/notify-upcoming")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'REFEREE', 'ADMIN')")
    @Operation(summary = "Summon the athletes of a bout: notifies them their bout starts in N minutes")
    public ApiResponse<List<NotificationDTO>> notifyUpcomingBout(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @Valid @RequestBody NotifyUpcomingBoutRequest request) {
        return new ApiResponse<>(true, "Atletas convocados correctamente",
                notificationService.notifyUpcomingBout(email, boutId, request));
    }

    @GetMapping("/api/notifications/mine")
    @Operation(summary = "Notification history of the authenticated user (newest first)")
    public ApiResponse<List<NotificationDTO>> getMyNotifications(@AuthenticationPrincipal String email) {
        return new ApiResponse<>(true, "Notificaciones obtenidas correctamente",
                notificationService.getMyNotifications(email));
    }

    @PutMapping("/api/notifications/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<NotificationDTO> markAsRead(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        return new ApiResponse<>(true, "Notificación marcada como leída",
                notificationService.markAsRead(email, id));
    }

    @PutMapping("/api/notifications/read-all")
    @Operation(summary = "Mark all notifications of the authenticated user as read")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal String email) {
        notificationService.markAllAsRead(email);
        return new ApiResponse<>(true, "Todas las notificaciones marcadas como leídas", null);
    }
}
