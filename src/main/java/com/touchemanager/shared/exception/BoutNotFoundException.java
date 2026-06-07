package com.touchemanager.shared.exception;

public class BoutNotFoundException extends RuntimeException {

    public BoutNotFoundException(Long id) {
        super("Bout not found with ID: " + id);
    }
}
