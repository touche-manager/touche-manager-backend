package com.touchemanager.auth.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
        Long id,
        String email,
        boolean active,
        LocalDateTime createdAt,
        List<String> roles
) {}
