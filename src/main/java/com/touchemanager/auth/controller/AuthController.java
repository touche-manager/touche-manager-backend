package com.touchemanager.auth.controller;

import com.touchemanager.auth.dto.RegisterRequestDTO;
import com.touchemanager.auth.dto.RegisterResponseDTO;
import com.touchemanager.auth.service.UsuarioService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "User registration and authentication")
public class AuthController {

    private final UsuarioService usuarioService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO response = usuarioService.registrar(request);
        return new ApiResponse<>(true, "User registered successfully", response);
    }
}
