package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.NombreRol;
import java.util.Set;

public record UserProfileDTO(
        Long id,
        String email,
        Set<NombreRol> roles,
        String profilePictureUrl
) {}
