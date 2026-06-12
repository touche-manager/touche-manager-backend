package com.touchemanager.auth.service;

import com.touchemanager.auth.dto.AdminPendingDocumentResponse;
import com.touchemanager.auth.dto.AdminStatsResponse;
import com.touchemanager.auth.dto.AdminUserResponse;
import com.touchemanager.auth.dto.UpdateUserRoleRequest;
import com.touchemanager.tournament.dto.DocumentValidationRequest;

import java.util.List;

public interface AdminService {

    List<AdminUserResponse> getAllUsers();

    AdminUserResponse updateUserRole(Long userId, UpdateUserRoleRequest request);

    List<AdminPendingDocumentResponse> getPendingDocuments();

    AdminPendingDocumentResponse validateDocument(Long documentId, DocumentValidationRequest request);

    AdminStatsResponse getStats();
}
