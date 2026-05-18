package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.NombreRol;

import java.util.Set;

public record RegisterResponseDTO(
        Long id,
        String email,
        Set<NombreRol> roles
) {}
