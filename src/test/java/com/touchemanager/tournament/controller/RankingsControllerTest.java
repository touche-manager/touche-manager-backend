package com.touchemanager.tournament.controller;

import com.touchemanager.auth.service.JwtService;
import com.touchemanager.auth.service.impl.UserDetailsServiceImpl;
import com.touchemanager.shared.security.JwtAuthenticationFilter;
import com.touchemanager.tournament.repository.TournamentSpecification;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.PouleService;
import com.touchemanager.tournament.service.RankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for RankingsController — covers filter binding, date format handling,
 * and invalid enum/date → 400 behaviour via GlobalExceptionHandler.
 */
@WebMvcTest(controllers = RankingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class RankingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private TournamentRepository tournamentRepository;
    @MockBean private PouleService          pouleService;
    @MockBean private RankingService        rankingService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private JwtService            jwtService;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    // ── /api/results ──────────────────────────────────────────────────────────

    @Test
    void getResults_noFilters_returnsOk() throws Exception {
        when(tournamentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getResults_withValidFilters_returnsOk() throws Exception {
        when(tournamentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/results")
                        .param("category", "SENIOR")
                        .param("gender",   "MALE")
                        .param("weapon",   "FOIL")
                        .param("dateFrom", "2025-01-01")
                        .param("dateTo",   "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getResults_invalidEnumCategory_returns400() throws Exception {
        mockMvc.perform(get("/api/results")
                        .param("category", "INVALID_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("category")));
    }

    @Test
    void getResults_invalidEnumWeapon_returns400() throws Exception {
        mockMvc.perform(get("/api/results")
                        .param("weapon", "LIGHTSABER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("weapon")));
    }

    @Test
    void getResults_invalidEnumGender_returns400() throws Exception {
        mockMvc.perform(get("/api/results")
                        .param("gender", "ROBOT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("gender")));
    }

    @Test
    void getResults_invalidDateFormat_returns400() throws Exception {
        // Sending date in DD/MM/YYYY format — backend expects ISO YYYY-MM-DD
        mockMvc.perform(get("/api/results")
                        .param("dateFrom", "15/06/2025"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getResults_dateWithTimestamp_returns400() throws Exception {
        // Sending datetime instead of date
        mockMvc.perform(get("/api/results")
                        .param("dateFrom", "2025-06-15T10:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── /api/rankings (alias) ─────────────────────────────────────────────────

    @Test
    void getRankingsAlias_withValidFilters_returnsOk() throws Exception {
        when(tournamentRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/rankings")
                        .param("weapon", "EPEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getRankingsAlias_invalidEnum_returns400() throws Exception {
        mockMvc.perform(get("/api/rankings")
                        .param("weapon", "SWORD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── /api/rankings/points ──────────────────────────────────────────────────

    @Test
    void getRankingPoints_noFilters_returnsOk() throws Exception {
        when(rankingService.getRankings(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/rankings/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getRankingPoints_withFilters_returnsOk() throws Exception {
        when(rankingService.getRankings(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/rankings/points")
                        .param("category", "JUNIOR")
                        .param("gender",   "FEMALE")
                        .param("weapon",   "SABRE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getRankingPoints_invalidEnum_returns400() throws Exception {
        mockMvc.perform(get("/api/rankings/points")
                        .param("category", "TODDLER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("category")));
    }
}
