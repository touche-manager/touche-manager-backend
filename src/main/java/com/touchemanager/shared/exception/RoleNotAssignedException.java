package com.touchemanager.shared.exception;

public class RoleNotAssignedException extends RuntimeException {

    public RoleNotAssignedException(String role) {
        super("Role not assigned to this user: " + role);
    }
}
