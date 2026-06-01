package com.touchemanager.tournament.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
        @NotBlank(message = "Payment ID is required")
        String paymentId
) {}
