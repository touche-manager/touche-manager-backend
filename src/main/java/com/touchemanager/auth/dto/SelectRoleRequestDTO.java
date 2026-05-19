package com.touchemanager.auth.dto;

import com.touchemanager.auth.entity.NombreRol;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectRoleRequestDTO {

    @NotNull(message = "Role must not be null")
    private NombreRol rol;
}
