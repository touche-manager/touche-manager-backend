package com.touchemanager.tournament.service.impl;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.touchemanager.shared.config.MercadoPagoProperties;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoServiceImpl implements PaymentService {

    private final MercadoPagoProperties mpProperties;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public String createPaymentLink(Enrollment enrollment) {
        if (!mpProperties.isEnabled()) {
            // Fallback to local simulator
            return "/athlete/enrollments/pay?id=" + enrollment.getId();
        }

        try {
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Inscripción: " + enrollment.getTournament().getName())
                    .quantity(1)
                    .unitPrice(enrollment.getAmount())
                    .build();

            String successUrl = mpProperties.getFrontendUrl() + "/athlete/enrollments?paymentStatus=success";
            String failureUrl = mpProperties.getFrontendUrl() + "/athlete/enrollments?paymentStatus=failure";
            String pendingUrl = mpProperties.getFrontendUrl() + "/athlete/enrollments?paymentStatus=pending";

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            // Notify our backend webhook
            String notificationUrl = mpProperties.getBackendUrl() + "/api/payments/webhook";

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference(enrollment.getId().toString())
                    .autoReturn("approved")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException e) {
            log.error("Mercado Pago API Error: Status Code: {}, Response content: {}",
                    e.getApiResponse() != null ? e.getApiResponse().getStatusCode() : "N/A",
                    e.getApiResponse() != null ? e.getApiResponse().getContent() : "none", e);
            throw new RuntimeException("Error en Mercado Pago API: " + 
                    (e.getApiResponse() != null ? e.getApiResponse().getContent() : e.getMessage()), e);
        } catch (Exception e) {
            log.error("Error generating Mercado Pago preference for enrollment ID {}", enrollment.getId(), e);
            throw new RuntimeException("Error al generar el link de pago de Mercado Pago", e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String paymentId) {
        if (!mpProperties.isEnabled()) {
            log.warn("Mercado Pago webhook triggered but integration is disabled");
            return;
        }

        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));

            if (payment == null) {
                log.error("No payment found on Mercado Pago for paymentId: {}", paymentId);
                return;
            }

            String externalRef = payment.getExternalReference();
            if (externalRef == null || externalRef.isBlank()) {
                log.warn("No external reference found on payment ID: {}", paymentId);
                return;
            }

            Long enrollmentId = Long.parseLong(externalRef);
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);
            if (enrollment == null) {
                log.error("No enrollment found matching external reference ID: {}", enrollmentId);
                return;
            }

            if ("approved".equals(payment.getStatus())) {
                if (enrollment.getStatus() != EnrollmentStatus.PAID) {
                    enrollment.setStatus(EnrollmentStatus.PAID);
                    enrollment.setPaymentId(paymentId);
                    enrollmentRepository.save(enrollment);
                    log.info("Enrollment ID {} marked as PAID via Mercado Pago. Payment ID: {}", enrollmentId, paymentId);
                }
            } else {
                log.info("Payment webhook processed for paymentId: {}. Payment status is: {}", paymentId, payment.getStatus());
            }

        } catch (Exception e) {
            log.error("Error processing Mercado Pago webhook notification for paymentId {}", paymentId, e);
            throw new RuntimeException("Error al procesar la notificación de pago", e);
        }
    }
}
