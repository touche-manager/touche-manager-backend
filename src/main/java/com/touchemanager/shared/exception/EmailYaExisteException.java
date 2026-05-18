package com.touchemanager.shared.exception;

public class EmailYaExisteException extends RuntimeException {

    public EmailYaExisteException(String email) {
        super("Email already registered: " + email);
    }
}
