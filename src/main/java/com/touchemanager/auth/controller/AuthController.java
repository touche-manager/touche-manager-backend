package com.touchemanager.auth.controller;

import com.touchemanager.auth.dto.*;
import com.touchemanager.auth.service.UserService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "User registration and authentication")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public ApiResponse<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO response = userService.register(request);
        return new ApiResponse<>(true, "User registered successfully", response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = userService.login(request);
        return new ApiResponse<>(true, "Login successful", response);
    }

    @PostMapping("/select-role")
    @Operation(summary = "Select active role when user has multiple roles",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<LoginResponseDTO> selectRole(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody SelectRoleRequestDTO request) {
        LoginResponseDTO response = userService.selectRole(email, request);
        return new ApiResponse<>(true, "Role selected successfully", response);
    }
}
