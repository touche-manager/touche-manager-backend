package com.touchemanager.athlete.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchemanager.athlete.dto.AthleteDocumentResponse;
import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.athlete.service.AthleteDocumentService;
import com.touchemanager.auth.service.JwtService;
import com.touchemanager.auth.service.impl.UserDetailsServiceImpl;
import com.touchemanager.shared.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AthleteDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AthleteDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AthleteDocumentService athleteDocumentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private AthleteDocumentResponse docResponse;
    private String email;

    @BeforeEach
    void setUp() {
        email = "athlete@test.com";

        docResponse = new AthleteDocumentResponse(
                100L,
                10L,
                "test.pdf",
                "application/pdf",
                DocumentType.MEDICAL_CLEARANCE,
                "Test Desc",
                LocalDateTime.now()
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
    void uploadDocument_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        when(athleteDocumentService.uploadDocument(eq(email), any(), eq(DocumentType.MEDICAL_CLEARANCE), eq("Test Desc")))
                .thenReturn(docResponse);

        mockMvc.perform(multipart("/api/athletes/profile/documents")
                        .file(file)
                        .param("documentType", "MEDICAL_CLEARANCE")
                        .param("description", "Test Desc")
                        .with(athleteUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.fileName").value("test.pdf"));

        verify(athleteDocumentService, times(1))
                .uploadDocument(eq(email), any(), eq(DocumentType.MEDICAL_CLEARANCE), eq("Test Desc"));
    }

    @Test
    void getMyDocuments_Success() throws Exception {
        when(athleteDocumentService.getMyDocuments(email)).thenReturn(List.of(docResponse));

        mockMvc.perform(get("/api/athletes/profile/documents")
                        .with(athleteUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Documents retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(100L));

        verify(athleteDocumentService, times(1)).getMyDocuments(email);
    }

    @Test
    void downloadDocument_Success() throws Exception {
        when(athleteDocumentService.getDocumentById(email, 100L)).thenReturn(docResponse);
        when(athleteDocumentService.getDocumentFile(email, 100L))
                .thenReturn(new ByteArrayInputStream("test content".getBytes()));

        mockMvc.perform(get("/api/athletes/profile/documents/100")
                        .with(athleteUser()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"test.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        verify(athleteDocumentService, times(1)).getDocumentById(email, 100L);
        verify(athleteDocumentService, times(1)).getDocumentFile(email, 100L);
    }

    @Test
    void deleteDocument_Success() throws Exception {
        doNothing().when(athleteDocumentService).deleteDocument(email, 100L);

        mockMvc.perform(delete("/api/athletes/profile/documents/100")
                        .with(athleteUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document deleted successfully"));

        verify(athleteDocumentService, times(1)).deleteDocument(email, 100L);
    }

    @Test
    void getAthleteDocuments_Success() throws Exception {
        when(athleteDocumentService.getAthleteDocuments(10L)).thenReturn(List.of(docResponse));

        mockMvc.perform(get("/api/athletes/10/documents")
                        .with(athleteUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(100L));

        verify(athleteDocumentService, times(1)).getAthleteDocuments(10L);
    }
}
