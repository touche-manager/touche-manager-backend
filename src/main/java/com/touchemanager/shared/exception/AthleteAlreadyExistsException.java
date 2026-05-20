package com.touchemanager.shared.exception;

public class AthleteAlreadyExistsException extends RuntimeException {

    public AthleteAlreadyExistsException(String email) {
        super("Athlete profile already exists for user: " + email);
    }
}
