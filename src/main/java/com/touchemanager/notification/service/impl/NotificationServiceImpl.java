package com.touchemanager.notification.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.notification.dto.NotificationDTO;
import com.touchemanager.notification.dto.NotifyUpcomingBoutRequest;
import com.touchemanager.notification.entity.Notification;
import com.touchemanager.notification.entity.NotificationType;
import com.touchemanager.notification.repository.NotificationRepository;
import com.touchemanager.notification.service.NotificationService;
import com.touchemanager.notification.sse.NotificationSseEmitterRegistry;
import com.touchemanager.shared.exception.BoutNotFoundException;
import com.touchemanager.shared.exception.NotificationNotFoundException;
import com.touchemanager.shared.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.touchemanager.notification.fcm.FcmService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    // Types that trigger FCM push (app closed / device locked)
    private static final Set<NotificationType> FCM_PRIORITY_TYPES;
    static {
        Set<NotificationType> types = new HashSet<>();
        types.add(NotificationType.UPCOMING_BOUT);
        types.add(NotificationType.YOUR_TURN);
        types.add(NotificationType.NEXT_UP);
        FCM_PRIORITY_TYPES = Collections.unmodifiableSet(types);
    }

    private final NotificationRepository notificationRepository;
    private final BoutRepository boutRepository;
    private final UserRepository userRepository;
    private final NotificationSseEmitterRegistry sseEmitterRegistry;
    private final FcmService fcmService;

    @Override
    @Transactional
    public List<NotificationDTO> notifyUpcomingBout(String requesterEmail, Long boutId, NotifyUpcomingBoutRequest request) {
        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(() -> new BoutNotFoundException(boutId));

        // Persist the piste on the bout when provided in the summon
        if (request.getPiste() != null && !request.getPiste().isBlank()) {
            bout.setPiste(request.getPiste());
            boutRepository.save(bout);
        }

        String piste = bout.getPiste() != null ? bout.getPiste() : "pista a confirmar";

        List<NotificationDTO> sent = new ArrayList<>();
        Athlete left = bout.getAthleteLeft();
        Athlete right = bout.getAthleteRight();

        if (left != null && right != null) {
            sent.add(createAndPush(bout, left, right, request.getMinutesAhead(), piste));
            sent.add(createAndPush(bout, right, left, request.getMinutesAhead(), piste));
        } else if (left != null) {
            // BYE bout — only the left athlete exists
            sent.add(createAndPush(bout, left, null, request.getMinutesAhead(), piste));
        }

        log.info("Upcoming-bout notification sent for bout {} by {} ({} recipients)",
                boutId, requesterEmail, sent.size());
        return sent;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(String email) {
        User user = getUser(email);
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public NotificationDTO markAsRead(String email, Long notificationId) {
        User user = getUser(email);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getRecipientUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to the authenticated user");
        }

        notification.setRead(true);
        return toDto(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        User user = getUser(email);
        notificationRepository.markAllAsRead(user.getId());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private NotificationDTO createAndPush(Bout bout, Athlete recipient, Athlete opponent, int minutesAhead, String piste) {
        String opponentName = opponent != null
                ? opponent.getFirstName() + " " + opponent.getLastName()
                : "rival a confirmar";

        String message = String.format("En %d minutos tenés un combate contra %s en %s",
                minutesAhead, opponentName, piste);

        Notification notification = new Notification();
        notification.setRecipientUserId(recipient.getUser().getId());
        notification.setTournamentId(bout.getTournament().getId());
        notification.setBoutId(bout.getId());
        notification.setType(NotificationType.UPCOMING_BOUT);
        notification.setMessage(message);

        NotificationDTO dto = toDto(notificationRepository.save(notification));

        // Push via SSE if the user has the app open
        sseEmitterRegistry.send(recipient.getUser().getId(), dto);

        // FCM push: upcoming bout is always high priority
        fcmService.sendToUser(
            recipient.getUser().getId(),
            "Touché Manager",
            String.format("En %d minutos: combate en %s", minutesAhead, piste),
            Map.of("type", NotificationType.UPCOMING_BOUT.name(),
                    "boutId", bout.getId().toString())
        );

        return dto;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    private NotificationDTO toDto(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getTournamentId(),
                n.getBoutId(),
                n.getType(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public NotificationDTO sendNotification(Long recipientUserId, Long tournamentId, Long boutId, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setTournamentId(tournamentId);
        notification.setBoutId(boutId);
        notification.setType(type);
        notification.setMessage(message);

        NotificationDTO dto = toDto(notificationRepository.save(notification));

        // Push via SSE if the user has the app open; notification is persisted in DB regardless
        sseEmitterRegistry.send(recipientUserId, dto);

        // FCM push for high-priority events (app closed / device locked)
        if (FCM_PRIORITY_TYPES.contains(type)) {
            fcmService.sendToUser(recipientUserId, "Touché Manager", message,
                    Map.of("type", type.name(),
                           "tournamentId", tournamentId != null ? tournamentId.toString() : ""));
        }

        log.info("Notification sent to user {}: {} ({})", recipientUserId, message, type);
        return dto;
    }
}
