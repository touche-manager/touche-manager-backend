package com.touchemanager.tournament.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touchemanager.shared.security.JwtAuthenticationFilter;
import com.touchemanager.tournament.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void handleWebhook_Success_WithJsonPayload() throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "payment",
                "action", "payment.created",
                "data", Map.of("id", "123456789")
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).handleWebhook("123456789");
    }

    @Test
    void handleWebhook_Success_WithQueryParams() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .with(csrf())
                        .param("id", "987654321")
                        .param("topic", "payment")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).handleWebhook("987654321");
    }

    @Test
    void handleWebhook_Ignore_WhenNotPayment() throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "subscription",
                "action", "created",
                "data", Map.of("id", "111222333")
        );

        mockMvc.perform(post("/api/payments/webhook")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentService, never()).handleWebhook(anyString());
    }

    @Test
    void handleWebhook_Error_Returns500InternalServerError() throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "payment",
                "action", "payment.created",
                "data", Map.of("id", "123")
        );

        doThrow(new RuntimeException("Service failed")).when(paymentService).handleWebhook("123");

        mockMvc.perform(post("/api/payments/webhook")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError());
    }
}
