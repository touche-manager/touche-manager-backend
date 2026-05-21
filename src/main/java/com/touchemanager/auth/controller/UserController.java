package com.touchemanager.auth.controller;

import com.touchemanager.auth.dto.UserProfileDTO;
import com.touchemanager.auth.service.UsuarioService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Operations related to user account profile and picture")
public class UserController {

    private final UsuarioService usuarioService;

    @GetMapping("/profile")
    @Operation(summary = "Get the active user's profile details",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserProfileDTO> getProfile(@AuthenticationPrincipal String email) {
        UserProfileDTO response = usuarioService.getProfile(email);
        return new ApiResponse<>(true, "Profile details retrieved successfully", response);
    }

    @PostMapping(value = "/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or update the active user's profile picture",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserProfileDTO> uploadProfilePicture(
            @AuthenticationPrincipal String email,
            @RequestParam("file") MultipartFile file) {
        UserProfileDTO response = usuarioService.uploadProfilePicture(email, file);
        return new ApiResponse<>(true, "Profile picture uploaded successfully", response);
    }

    @DeleteMapping("/profile/picture")
    @Operation(summary = "Delete the active user's profile picture",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserProfileDTO> deleteProfilePicture(@AuthenticationPrincipal String email) {
        UserProfileDTO response = usuarioService.deleteProfilePicture(email);
        return new ApiResponse<>(true, "Profile picture deleted successfully", response);
    }

    @GetMapping("/profile-picture/{userId}")
    @Operation(summary = "Download or view a user's profile picture (Public)")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable Long userId) {
        InputStream stream = usuarioService.getProfilePicture(userId);
        String contentType = usuarioService.getProfilePictureContentType(userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(stream));
    }
}
