package com.touchemanager.athlete.service.impl;

import com.touchemanager.athlete.dto.AthleteDocumentResponse;
import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.AthleteDocument;
import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.athlete.service.AthleteDocumentService;
import com.touchemanager.shared.service.FileStorageService;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.shared.exception.DocumentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AthleteDocumentServiceImpl implements AthleteDocumentService {

    private final AthleteDocumentRepository athleteDocumentRepository;
    private final AthleteRepository athleteRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public AthleteDocumentResponse uploadDocument(String email, MultipartFile file, DocumentType type, String description) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        String directory = "athletes/" + athlete.getId() + "/" + type.name().toLowerCase();
        String fileKey = fileStorageService.uploadFile(file, directory);

        AthleteDocument document = new AthleteDocument();
        document.setAthlete(athlete);
        document.setFileKey(fileKey);
        document.setContentType(file.getContentType());
        document.setDocumentType(type);
        document.setDescription(description);
        document.setUploadDate(LocalDateTime.now());

        AthleteDocument saved = athleteDocumentRepository.save(document);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AthleteDocumentResponse> getMyDocuments(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        return athleteDocumentRepository.findByAthleteId(athlete.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AthleteDocumentResponse> getAthleteDocuments(Long athleteId) {
        return athleteDocumentRepository.findByAthleteId(athleteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AthleteDocumentResponse getDocumentById(String email, Long documentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getAthlete().getId().equals(athlete.getId())) {
            throw new AccessDeniedException("You do not have permission to access this document");
        }

        return mapToResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public AthleteDocumentResponse getDocumentByIdAsOrganizer(Long athleteId, Long documentId) {
        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getAthlete().getId().equals(athleteId)) {
            throw new AccessDeniedException("Document does not belong to the specified athlete");
        }

        return mapToResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getDocumentFile(String email, Long documentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getAthlete().getId().equals(athlete.getId())) {
            throw new AccessDeniedException("You do not have permission to access this document");
        }

        return fileStorageService.downloadFile(document.getFileKey());
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getDocumentFileAsOrganizer(Long athleteId, Long documentId) {
        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getAthlete().getId().equals(athleteId)) {
            throw new AccessDeniedException("Document does not belong to the specified athlete");
        }

        return fileStorageService.downloadFile(document.getFileKey());
    }

    @Override
    @Transactional
    public void deleteDocument(String email, Long documentId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Athlete athlete = athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));

        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getAthlete().getId().equals(athlete.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this document");
        }

        fileStorageService.deleteFile(document.getFileKey());
        athleteDocumentRepository.delete(document);
    }

    private AthleteDocumentResponse mapToResponse(AthleteDocument document) {
        return new AthleteDocumentResponse(
                document.getId(),
                document.getAthlete().getId(),
                document.getContentType(),
                document.getDocumentType(),
                document.getDescription(),
                document.getUploadDate()
        );
    }
}
