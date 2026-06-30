package com.touchemanager.tournament.controller;

import com.touchemanager.tournament.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments Webhook — Public")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Receive payment notifications from Mercado Pago (Webhooks)")
    public ResponseEntity<Void> handleMercadoPagoWebhook(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestParam(value = "id", required = false) String paramId,
            @RequestParam(value = "topic", required = false) String paramTopic) {

        log.info("Mercado Pago webhook received. Payload: {}, Query params: id={}, topic={}", payload, paramId, paramTopic);

        // Mercado Pago sends notifications in various formats depending on Webhook vs IPN:
        // 1. Webhook (Standard):
        //    Body contains: { "type": "payment", "action": "payment.created", "data": { "id": "123456789" } }
        // 2. IPN (Legacy or Fallback):
        //    Query parameters contain: ?id=123456789&topic=payment

        String paymentId = null;
        String type = null;

        // Try extracting from JSON body (Webhook)
        if (payload != null) {
            type = (String) payload.get("type");
            if (payload.get("data") instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) payload.get("data");
                if (data.containsKey("id")) {
                    paymentId = String.valueOf(data.get("id"));
                }
            }
        }

        // Try extracting from query params (IPN) if not found in body
        if (paymentId == null && paramId != null && "payment".equals(paramTopic)) {
            paymentId = paramId;
            type = "payment";
        }

        if (paymentId != null && "payment".equals(type)) {
            try {
                paymentService.handleWebhook(paymentId);
            } catch (Exception e) {
                log.error("Error processing webhook for payment ID: {}", paymentId, e);
                // Return 500 so Mercado Pago retries later
                return ResponseEntity.status(500).build();
            }
        } else {
            log.warn("Ignored Mercado Pago webhook. Not a payment notification or empty ID. Type: {}, ID: {}", type, paymentId);
        }

        // Always return 200 OK to Mercado Pago to acknowledge receipt
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-from-redirect")
    @Operation(summary = "Synchronously confirm payment via query param fallback")
    public ResponseEntity<Void> confirmFromRedirect(@RequestParam("payment_id") String paymentId) {
        log.info("Manual payment confirmation requested for payment ID: {}", paymentId);
        try {
            paymentService.handleWebhook(paymentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing manual confirmation for payment ID: {}", paymentId, e);
            return ResponseEntity.status(500).build();
        }
    }
}
