package com.touchemanager.auth.dto;

import com.touchemanager.athlete.entity.DocumentType;

import java.time.LocalDateTime;

public record AdminPendingDocumentResponse(
        Long documentId,
        Long athleteId,
        String athleteName,
        String dni,
        String club,
        DocumentType documentType,
        String fileKey,
        String description,
        LocalDateTime uploadDate
) {}
