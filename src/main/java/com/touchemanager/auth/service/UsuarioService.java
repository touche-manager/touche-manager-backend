package com.touchemanager.auth.service;

import com.touchemanager.auth.dto.RegisterRequestDTO;
import com.touchemanager.auth.dto.RegisterResponseDTO;

public interface UsuarioService {

    RegisterResponseDTO registrar(RegisterRequestDTO request);
}
