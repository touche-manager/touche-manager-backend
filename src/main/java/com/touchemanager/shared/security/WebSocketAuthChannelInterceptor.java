package com.touchemanager.shared.security;

import com.touchemanager.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates STOMP CONNECT frames with the JWT sent in the "Authorization"
 * native header. The resulting Principal name is the user id, so server-side
 * code can target a single user with convertAndSendToUser(userId, ...).
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing Authorization header in STOMP CONNECT");
            }

            String token = authHeader.substring(7);
            if (!jwtService.isTokenValid(token)) {
                throw new IllegalArgumentException("Invalid or expired JWT in STOMP CONNECT");
            }

            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities));
        }

        return message;
    }
}
