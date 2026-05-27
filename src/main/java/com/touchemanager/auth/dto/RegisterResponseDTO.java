package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.RoleName;

import java.util.Set;

public record RegisterResponseDTO(
        Long id,
        String email,
        Set<RoleName> roles
) {}
