package com.touchemanager.notification.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "firebase")
@Getter
@Setter
@Slf4j
public class FcmProperties {

    private String credentialsPath = "";
    private String credentialsJson = "";
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream credentialsStream = resolveCredentials();
            if (credentialsStream == null) {
                log.warn("Firebase credentials not configured. Push notifications via FCM will be disabled.");
                return;
            }
            try {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                        .build();
                FirebaseApp.initializeApp(options);
                initialized = true;
                log.info("Firebase Admin SDK initialized successfully.");
            } catch (Exception e) {
                log.error("Failed to initialize Firebase Admin SDK", e);
            }
        } else {
            initialized = true;
        }
    }

    public boolean isEnabled() {
        return initialized;
    }

    private InputStream resolveCredentials() {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try {
                return new FileInputStream(credentialsPath);
            } catch (Exception e) {
                log.error("Could not read Firebase credentials file at: {}", credentialsPath, e);
            }
        }
        return null;
    }
}
