package com.touchemanager.auth.service;

import com.touchemanager.auth.dto.*;

public interface UsuarioService {

    RegisterResponseDTO registrar(RegisterRequestDTO request);

    LoginResponseDTO login(LoginRequestDTO request);

    LoginResponseDTO selectRole(String email, SelectRoleRequestDTO request);
}
