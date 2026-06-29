package com.touchemanager.tournament.service;

import com.touchemanager.tournament.entity.Enrollment;

public interface PaymentService {
    /**
     * Generates a payment link for the given enrollment.
     * Can be a remote Mercado Pago preference checkout URL or a local simulator link.
     */
    String createPaymentLink(Enrollment enrollment);

    /**
     * Processes a payment update webhook from Mercado Pago.
     * @param paymentId the ID of the payment transaction to verify and process.
     */
    void handleWebhook(String paymentId);
}
