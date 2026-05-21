package com.touchemanager.athlete.service;

import com.touchemanager.athlete.dto.AthleteDocumentResponse;
import com.touchemanager.athlete.entity.DocumentType;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

public interface AthleteDocumentService {
    AthleteDocumentResponse uploadDocument(String email, MultipartFile file, DocumentType type, String description);
    List<AthleteDocumentResponse> getMyDocuments(String email);
    List<AthleteDocumentResponse> getAthleteDocuments(Long athleteId);
    AthleteDocumentResponse getDocumentById(String email, Long documentId);
    AthleteDocumentResponse getDocumentByIdAsOrganizer(Long athleteId, Long documentId);
    InputStream getDocumentFile(String email, Long documentId);
    InputStream getDocumentFileAsOrganizer(Long athleteId, Long documentId);
    void deleteDocument(String email, Long documentId);
}
