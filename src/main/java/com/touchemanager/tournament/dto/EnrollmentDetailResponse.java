package com.touchemanager.tournament.dto;

import com.touchemanager.athlete.entity.DocumentValidationStatus;
import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.tournament.entity.EnrollmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EnrollmentDetailResponse(
        Long enrollmentId,
        EnrollmentStatus status,
        BigDecimal amount,
        LocalDateTime enrollmentDate,
        AthleteInfo athlete,
        List<DocumentInfo> documents
) {

    public record AthleteInfo(
            Long id,
            String firstName,
            String lastName,
            String dni,
            LocalDate birthDate,
            String club,
            String province
    ) {}

    public record DocumentInfo(
            Long documentId,
            DocumentType documentType,
            String fileKey,
            DocumentValidationStatus validationStatus,
            String reviewNotes,
            LocalDateTime uploadDate
    ) {}
}
