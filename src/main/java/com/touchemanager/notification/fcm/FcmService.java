package com.touchemanager.notification.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.touchemanager.notification.entity.FcmToken;
import com.touchemanager.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final FcmProperties fcmProperties;
    private final FcmTokenRepository fcmTokenRepository;

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        if (!fcmProperties.isEnabled()) {
            log.debug("FCM disabled — skipping push for user {}", userId);
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No FCM tokens found for user {}", userId);
            return;
        }

        for (FcmToken fcmToken : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(fcmToken.getToken());

                // We only use data payload so the Service Worker can fully customize the notification UI
                // and avoid showing a duplicate default notification.
                builder.putData("title", title);
                builder.putData("body", body);

                if (data != null && !data.isEmpty()) {
                    builder.putAllData(data);
                }

                String response = FirebaseMessaging.getInstance().send(builder.build());
                log.info("FCM push sent to user {} (token hint: ...{}): {}",
                        userId,
                        fcmToken.getDeviceHint() != null
                            ? fcmToken.getDeviceHint().substring(0, Math.min(fcmToken.getDeviceHint().length(), 30))
                            : "",
                        response);

            } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
                String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
                if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                    log.warn("FCM token invalid for user {}, removing from DB", userId);
                    fcmTokenRepository.delete(fcmToken);
                } else {
                    log.error("Failed to send FCM to user {}", userId, e);
                }
            } catch (Exception e) {
                log.error("Unexpected error sending FCM to user {}", userId, e);
            }
        }
    }
}
