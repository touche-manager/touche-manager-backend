package com.touchemanager.auth.controller;

import com.touchemanager.auth.dto.AdminPendingDocumentResponse;
import com.touchemanager.auth.dto.AdminStatsResponse;
import com.touchemanager.auth.dto.AdminUserResponse;
import com.touchemanager.auth.dto.UpdateUserRoleRequest;
import com.touchemanager.auth.service.AdminService;
import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.DocumentValidationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "List every user with their current roles (admin only)")
    public ApiResponse<List<AdminUserResponse>> getAllUsers() {
        return new ApiResponse<>(true, "Usuarios obtenidos correctamente",
                adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Grant or revoke a role for a user (admin only)")
    public ApiResponse<AdminUserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return new ApiResponse<>(true, "Rol actualizado correctamente",
                adminService.updateUserRole(id, request));
    }

    @GetMapping("/documents/pending")
    @Operation(summary = "List every pending athlete document across the system (admin only)")
    public ApiResponse<List<AdminPendingDocumentResponse>> getPendingDocuments() {
        return new ApiResponse<>(true, "Documentos pendientes obtenidos correctamente",
                adminService.getPendingDocuments());
    }

    @PutMapping("/documents/{id}/validate")
    @Operation(summary = "Approve or reject an athlete document (admin only)")
    public ApiResponse<AdminPendingDocumentResponse> validateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentValidationRequest request) {
        return new ApiResponse<>(true, "Documento validado correctamente",
                adminService.validateDocument(id, request));
    }

    @GetMapping("/stats")
    @Operation(summary = "General system statistics: users, athletes, tournaments, enrollments (admin only)")
    public ApiResponse<AdminStatsResponse> getStats() {
        return new ApiResponse<>(true, "Estadísticas obtenidas correctamente",
                adminService.getStats());
    }
}
