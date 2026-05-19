package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.NombreRol;

import java.util.Set;

/**
 * Response for POST /api/auth/login.
 * - Single role: token is populated, roles is null.
 * - Multiple roles: roles is populated, token is null (client must call select-role).
 */
public record LoginResponseDTO(
        String token,
        Set<NombreRol> roles
) {}
