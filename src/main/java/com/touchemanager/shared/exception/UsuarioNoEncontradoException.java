package com.touchemanager.shared.exception;

public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(String identifier) {
        super("User not found: " + identifier);
    }
}
