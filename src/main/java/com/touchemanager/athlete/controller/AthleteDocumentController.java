package com.touchemanager.athlete.controller;

import com.touchemanager.athlete.dto.AthleteDocumentResponse;
import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.athlete.service.AthleteDocumentService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/athletes")
@RequiredArgsConstructor
@Tag(name = "Athlete Documents")
public class AthleteDocumentController {

    private final AthleteDocumentService athleteDocumentService;

    @PostMapping(value = "/profile/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a document for the authenticated athlete")
    public ApiResponse<AthleteDocumentResponse> uploadDocument(
            @AuthenticationPrincipal String email,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "description", required = false) String description) {
        AthleteDocumentResponse response = athleteDocumentService.uploadDocument(email, file, documentType, description);
        return new ApiResponse<>(true, "Document uploaded successfully", response);
    }

    @GetMapping("/profile/documents")
    @Operation(summary = "Get all documents of the authenticated athlete")
    public ApiResponse<List<AthleteDocumentResponse>> getMyDocuments(@AuthenticationPrincipal String email) {
        List<AthleteDocumentResponse> response = athleteDocumentService.getMyDocuments(email);
        return new ApiResponse<>(true, "Documents retrieved successfully", response);
    }

    @GetMapping("/profile/documents/{documentId}")
    @Operation(summary = "Download a specific document of the authenticated athlete")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal String email,
            @PathVariable Long documentId) {
        AthleteDocumentResponse metadata = athleteDocumentService.getDocumentById(email, documentId);
        InputStream stream = athleteDocumentService.getDocumentFile(email, documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.fileName() + "\"")
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/profile/documents/{documentId}")
    @Operation(summary = "Delete a specific document of the authenticated athlete")
    public ApiResponse<Void> deleteDocument(
            @AuthenticationPrincipal String email,
            @PathVariable Long documentId) {
        athleteDocumentService.deleteDocument(email, documentId);
        return new ApiResponse<>(true, "Document deleted successfully", null);
    }

    @GetMapping("/{athleteId}/documents")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMIN')")
    @Operation(summary = "Get all documents of a specific athlete (Organizers/Admins only)")
    public ApiResponse<List<AthleteDocumentResponse>> getAthleteDocuments(@PathVariable Long athleteId) {
        List<AthleteDocumentResponse> response = athleteDocumentService.getAthleteDocuments(athleteId);
        return new ApiResponse<>(true, "Athlete documents retrieved successfully", response);
    }

    @GetMapping("/{athleteId}/documents/{documentId}")
    @PreAuthorize("hasAnyRole('ORGANIZADOR', 'ADMIN')")
    @Operation(summary = "Download a specific document of an athlete (Organizers/Admins only)")
    public ResponseEntity<Resource> downloadAthleteDocument(
            @PathVariable Long athleteId,
            @PathVariable Long documentId) {
        AthleteDocumentResponse metadata = athleteDocumentService.getDocumentByIdAsOrganizer(athleteId, documentId);
        InputStream stream = athleteDocumentService.getDocumentFileAsOrganizer(athleteId, documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.fileName() + "\"")
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .body(new InputStreamResource(stream));
    }
}
