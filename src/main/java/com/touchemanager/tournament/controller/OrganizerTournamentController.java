package com.touchemanager.tournament.controller;

import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.DocumentValidationRequest;
import com.touchemanager.tournament.dto.EnrollmentDetailResponse;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import com.touchemanager.tournament.dto.TournamentRequest;
import com.touchemanager.tournament.service.OrganizerTournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizer/tournaments")
@RequiredArgsConstructor
@Tag(name = "Organizer — Tournament Management")
public class OrganizerTournamentController {

    private final OrganizerTournamentService organizerTournamentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get all tournaments created by the authenticated organizer")
    public ApiResponse<List<OrganizerTournamentResponse>> getMyTournaments(
            @AuthenticationPrincipal String email) {
        return new ApiResponse<>(true, "Tournaments retrieved successfully",
                organizerTournamentService.getMyTournaments(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get a specific tournament owned by the organizer")
    public ApiResponse<OrganizerTournamentResponse> getTournamentById(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        return new ApiResponse<>(true, "Tournament retrieved successfully",
                organizerTournamentService.getTournamentById(email, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Create a new tournament")
    public ApiResponse<OrganizerTournamentResponse> createTournament(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody TournamentRequest request) {
        return new ApiResponse<>(true, "Tournament created successfully",
                organizerTournamentService.createTournament(email, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Update an existing tournament")
    public ApiResponse<OrganizerTournamentResponse> updateTournament(
            @AuthenticationPrincipal String email,
            @PathVariable Long id,
            @Valid @RequestBody TournamentRequest request) {
        return new ApiResponse<>(true, "Tournament updated successfully",
                organizerTournamentService.updateTournament(email, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Delete a tournament")
    public void deleteTournament(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        organizerTournamentService.deleteTournament(email, id);
    }

    @GetMapping("/{id}/enrollments")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get all enrollments for a tournament with athlete info and document status")
    public ApiResponse<List<EnrollmentDetailResponse>> getTournamentEnrollments(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        return new ApiResponse<>(true, "Enrollments retrieved successfully",
                organizerTournamentService.getTournamentEnrollments(email, id));
    }

    @PatchMapping("/documents/{documentId}/validate")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Approve or reject an athlete's document")
    public ApiResponse<Void> validateDocument(
            @AuthenticationPrincipal String email,
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentValidationRequest request) {
        organizerTournamentService.validateDocument(email, documentId, request);
        return new ApiResponse<>(true, "Document validation status updated", null);
    }
}
