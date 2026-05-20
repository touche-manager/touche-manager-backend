package com.touchemanager.athlete.controller;

import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;
import com.touchemanager.athlete.service.AthleteService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/athletes")
@RequiredArgsConstructor
@Tag(name = "Athlete")
public class AthleteController {

    private final AthleteService athleteService;

    @GetMapping("/profile")
    @Operation(summary = "Get the authenticated athlete's profile")
    public ApiResponse<AthleteResponse> getProfile(@AuthenticationPrincipal String email) {
        AthleteResponse response = athleteService.getProfile(email);
        return new ApiResponse<>(true, "Athlete profile retrieved successfully", response);
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an athlete profile for the authenticated user")
    public ApiResponse<AthleteResponse> createProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AthleteRequest request) {
        AthleteResponse response = athleteService.createProfile(email, request);
        return new ApiResponse<>(true, "Athlete profile created successfully", response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update the authenticated athlete's profile")
    public ApiResponse<AthleteResponse> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AthleteRequest request) {
        AthleteResponse response = athleteService.updateProfile(email, request);
        return new ApiResponse<>(true, "Athlete profile updated successfully", response);
    }
}
