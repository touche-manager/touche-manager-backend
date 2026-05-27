package com.touchemanager.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchemanager.auth.dto.*;
import com.touchemanager.auth.entity.RoleName;
import com.touchemanager.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.touchemanager.shared.security.JwtAuthenticationFilter;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequestDTO registerRequest;
    private RegisterResponseDTO registerResponse;
    private LoginRequestDTO loginRequest;
    private SelectRoleRequestDTO selectRoleRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO();
        registerRequest.setEmail("test@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setRoles(Set.of(RoleName.ATHLETE));

        registerResponse = new RegisterResponseDTO(1L, "test@test.com", Set.of(RoleName.ATHLETE));

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");

        selectRoleRequest = new SelectRoleRequestDTO();
        selectRoleRequest.setRole(RoleName.ATHLETE);
    }

    @Test
    void register_Success() throws Exception {
        when(userService.register(any(RegisterRequestDTO.class))).thenReturn(registerResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    void login_Success() throws Exception {
        LoginResponseDTO loginResponse = new LoginResponseDTO("token123", null);
        when(userService.login(any(LoginRequestDTO.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token123"));
    }

    @Test
    void selectRole_Success() throws Exception {
        LoginResponseDTO loginResponse = new LoginResponseDTO("token123", null);
        when(userService.selectRole(any(), any(SelectRoleRequestDTO.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRoleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("token123"));
    }
}
