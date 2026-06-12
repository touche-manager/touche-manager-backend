package com.touchemanager.tournament.controller;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.EnrollmentRequest;
import com.touchemanager.tournament.dto.EnrollmentResponse;
import com.touchemanager.tournament.dto.PaymentConfirmRequest;
import com.touchemanager.tournament.dto.PublicTournamentResponse;
import com.touchemanager.tournament.dto.TournamentResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.entity.Weapon;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import com.touchemanager.tournament.service.EnrollmentService;
import com.touchemanager.tournament.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournament")
public class TournamentController {

    private final TournamentService tournamentService;
    private final EnrollmentService enrollmentService;

    @GetMapping
    @Operation(summary = "Get all available tournaments with deadlines and prices for the current athlete")
    public ApiResponse<List<TournamentResponse>> getAvailableTournaments(@AuthenticationPrincipal String email) {
        List<TournamentResponse> response = tournamentService.getAvailableTournaments(email);
        return new ApiResponse<>(true, "Tournaments retrieved successfully", response);
    }

    @GetMapping("/public")
    @Operation(summary = "Public tournament search with optional filters (no auth required)")
    public ApiResponse<List<PublicTournamentResponse>> searchPublicTournaments(
            @RequestParam(required = false) TournamentPhase status,
            @RequestParam(required = false) Weapon weapon,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return new ApiResponse<>(true, "Torneos obtenidos correctamente",
                tournamentService.searchPublicTournaments(status, weapon, category, gender, dateFrom, dateTo));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament details by ID")
    public ApiResponse<TournamentResponse> getTournamentDetails(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        TournamentResponse response = tournamentService.getTournamentDetails(email, id);
        return new ApiResponse<>(true, "Tournament details retrieved successfully", response);
    }

    @PostMapping("/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a pre-enrollment for a tournament (requires valid docs)")
    public ApiResponse<EnrollmentResponse> enroll(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(email, request);
        return new ApiResponse<>(true, "Pre-enrollment created successfully", response);
    }

    @PostMapping("/enrollments/{enrollmentId}/confirm")
    @Operation(summary = "Confirm payment for a tournament pre-enrollment")
    public ApiResponse<EnrollmentResponse> confirmPayment(
            @AuthenticationPrincipal String email,
            @PathVariable Long enrollmentId,
            @Valid @RequestBody PaymentConfirmRequest request) {
        EnrollmentResponse response = enrollmentService.confirmPayment(email, enrollmentId, request.paymentId());
        return new ApiResponse<>(true, "Payment confirmed successfully", response);
    }

    @PostMapping("/enrollments/{enrollmentId}/cancel")
    @Operation(summary = "Cancel / unenroll from a tournament pre-enrollment or paid enrollment")
    public ApiResponse<EnrollmentResponse> cancelEnrollment(
            @AuthenticationPrincipal String email,
            @PathVariable Long enrollmentId) {
        EnrollmentResponse response = enrollmentService.cancelEnrollment(email, enrollmentId);
        return new ApiResponse<>(true, "Enrollment cancelled successfully", response);
    }
}
