package com.touchemanager.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchemanager.auth.dto.RegisterRequestDTO;
import com.touchemanager.auth.dto.RegisterResponseDTO;
import com.touchemanager.auth.entity.NombreRol;
import com.touchemanager.auth.service.UsuarioService;
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

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequestDTO validRequest;
    private RegisterResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequestDTO();
        validRequest.setEmail("test@test.com");
        validRequest.setPassword("password123");
        validRequest.setRoles(Set.of(NombreRol.ATLETA));

        responseDTO = new RegisterResponseDTO(1L, "test@test.com", Set.of(NombreRol.ATLETA));
    }

    @Test
    void register_Success() throws Exception {
        when(usuarioService.registrar(any(RegisterRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        validRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }
}
