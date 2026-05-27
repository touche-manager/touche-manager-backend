package com.touchemanager.shared.exception;

public class DniAlreadyExistsException extends RuntimeException {

    public DniAlreadyExistsException(String dni) {
        super("DNI already registered: " + dni);
    }
}
