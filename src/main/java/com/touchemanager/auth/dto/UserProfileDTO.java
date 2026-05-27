package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.RoleName;
import java.util.Set;

public record UserProfileDTO(
        Long id,
        String email,
        Set<RoleName> roles,
        String profilePictureUrl
) {}
