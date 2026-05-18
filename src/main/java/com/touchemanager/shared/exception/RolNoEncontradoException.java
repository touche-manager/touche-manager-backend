package com.touchemanager.shared.exception;

public class RolNoEncontradoException extends RuntimeException {

    public RolNoEncontradoException(String nombre) {
        super("Role not found: " + nombre);
    }
}
