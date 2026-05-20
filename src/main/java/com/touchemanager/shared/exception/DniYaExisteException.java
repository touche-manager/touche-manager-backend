package com.touchemanager.shared.exception;

public class DniYaExisteException extends RuntimeException {

    public DniYaExisteException(String dni) {
        super("DNI already registered: " + dni);
    }
}
