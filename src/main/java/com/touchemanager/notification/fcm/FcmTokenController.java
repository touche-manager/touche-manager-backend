package com.touchemanager.notification.fcm;

import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.notification.entity.FcmToken;
import com.touchemanager.notification.repository.FcmTokenRepository;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/fcm-token")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FCM Tokens")
public class FcmTokenController {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Register or refresh the FCM push token for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> registerToken(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            Authentication auth) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Token is required", null));
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String deviceHint = userAgent != null
                ? userAgent.substring(0, Math.min(userAgent.length(), 200))
                : "unknown";

        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElse(new FcmToken());
        fcmToken.setUserId(user.getId());
        fcmToken.setToken(token);
        fcmToken.setDeviceHint(deviceHint);
        fcmTokenRepository.save(fcmToken);

        log.info("FCM token registered for user {}", user.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "Token registrado", null));
    }

    @DeleteMapping
    @Operation(summary = "Remove the FCM token on logout")
    public ResponseEntity<ApiResponse<Void>> removeToken(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        String token = body.get("token");
        if (token != null && !token.isBlank()) {
            fcmTokenRepository.deleteByToken(token);
        }
        log.info("FCM token removed for user {}", auth.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "Token eliminado", null));
    }
}
