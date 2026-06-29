package com.touchemanager.tournament.service;

import com.touchemanager.tournament.dto.EnrollmentRequest;
import com.touchemanager.tournament.dto.EnrollmentResponse;

public interface EnrollmentService {
    EnrollmentResponse enroll(String email, EnrollmentRequest request);
    EnrollmentResponse confirmPayment(String email, Long enrollmentId, String paymentId);
    EnrollmentResponse cancelEnrollment(String email, Long enrollmentId);
    String getPaymentLink(String email, Long enrollmentId);
}
