package com.touchemanager.shared.exception;

public class RolNoAsignadoException extends RuntimeException {

    public RolNoAsignadoException(String rol) {
        super("Role not assigned to this user: " + rol);
    }
}
