package com.touchemanager.tournament.controller;

import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.RefereeApplicationResponse;
import com.touchemanager.tournament.dto.ReviewApplicationRequest;
import com.touchemanager.tournament.service.RefereeApplicationService;
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
@RequestMapping("/api/referee-applications")
@RequiredArgsConstructor
@Tag(name = "Referee Applications")
public class RefereeApplicationController {

    private final RefereeApplicationService refereeApplicationService;

    @PostMapping("/{tournamentId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Referee applies to officiate at a tournament")
    public ApiResponse<RefereeApplicationResponse> apply(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Postulacion enviada correctamente",
                refereeApplicationService.apply(email, tournamentId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Get all applications of the authenticated referee")
    public ApiResponse<List<RefereeApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal String email) {
        return new ApiResponse<>(true, "Postulaciones obtenidas correctamente",
                refereeApplicationService.getMyApplications(email));
    }

    @GetMapping("/tournament/{tournamentId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get all referee applications for a tournament")
    public ApiResponse<List<RefereeApplicationResponse>> getApplicationsForTournament(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Postulaciones obtenidas correctamente",
                refereeApplicationService.getApplicationsForTournament(email, tournamentId));
    }

    @PatchMapping("/{applicationId}/review")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Accept or reject a referee application")
    public ApiResponse<RefereeApplicationResponse> reviewApplication(
            @AuthenticationPrincipal String email,
            @PathVariable Long applicationId,
            @Valid @RequestBody ReviewApplicationRequest request) {
        return new ApiResponse<>(true, "Postulacion revisada correctamente",
                refereeApplicationService.reviewApplication(email, applicationId, request));
    }

    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Cancel own referee application")
    public void cancelApplication(
            @AuthenticationPrincipal String email,
            @PathVariable Long applicationId) {
        refereeApplicationService.cancelApplication(email, applicationId);
    }
}
