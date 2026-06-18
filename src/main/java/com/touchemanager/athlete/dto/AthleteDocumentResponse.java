package com.touchemanager.athlete.dto;

import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.athlete.entity.DocumentValidationStatus;
import java.time.LocalDateTime;

public record AthleteDocumentResponse(
        Long id,
        Long athleteId,
        String contentType,
        DocumentType documentType,
        String description,
        LocalDateTime uploadDate,
        DocumentValidationStatus validationStatus,
        String reviewNotes
) {}
