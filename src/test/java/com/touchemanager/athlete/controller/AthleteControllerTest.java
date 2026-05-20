package com.touchemanager.athlete.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;
import com.touchemanager.athlete.entity.DominantHand;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.athlete.service.AthleteService;
import com.touchemanager.auth.service.JwtService;
import com.touchemanager.auth.service.impl.UserDetailsServiceImpl;
import com.touchemanager.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AthleteController.class)
@AutoConfigureMockMvc(addFilters = false)
class AthleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AthleteService athleteService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private AthleteResponse athleteResponse;
    private AthleteRequest athleteRequest;
    private String email;

    @BeforeEach
    void setUp() {
        email = "athlete@test.com";

        athleteResponse = new AthleteResponse(
                10L,
                1L,
                email,
                "John",
                "Doe",
                "12345678",
                LocalDate.of(1995, 5, 15),
                Gender.MALE,
                DominantHand.RIGHT,
                "Fencing Club",
                "Buenos Aires"
        );

        athleteRequest = new AthleteRequest(
                "John",
                "Doe",
                "12345678",
                LocalDate.of(1995, 5, 15),
                Gender.MALE,
                DominantHand.RIGHT,
                "Fencing Club",
                "Buenos Aires"
        );
    }

    private RequestPostProcessor athleteUser() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList())
            );
            return request;
        };
    }

    @Test
    void getProfile_Success() throws Exception {
        when(athleteService.getProfile(email)).thenReturn(athleteResponse);

        mockMvc.perform(get("/api/athletes/profile")
                        .with(athleteUser())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Athlete profile retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"));

        verify(athleteService, times(1)).getProfile(email);
    }

    @Test
    void createProfile_Success() throws Exception {
        when(athleteService.createProfile(eq(email), any(AthleteRequest.class))).thenReturn(athleteResponse);

        mockMvc.perform(post("/api/athletes/profile")
                        .with(athleteUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(athleteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Athlete profile created successfully"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.firstName").value("John"));

        verify(athleteService, times(1)).createProfile(eq(email), any(AthleteRequest.class));
    }

    @Test
    void updateProfile_Success() throws Exception {
        when(athleteService.updateProfile(eq(email), any(AthleteRequest.class))).thenReturn(athleteResponse);

        mockMvc.perform(put("/api/athletes/profile")
                        .with(athleteUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(athleteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Athlete profile updated successfully"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.lastName").value("Doe"));

        verify(athleteService, times(1)).updateProfile(eq(email), any(AthleteRequest.class));
    }

    @Test
    void createProfile_ValidationError() throws Exception {
        AthleteRequest invalidRequest = new AthleteRequest(); // Blank fields, null birth date

        mockMvc.perform(post("/api/athletes/profile")
                        .with(athleteUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(athleteService);
    }
}
