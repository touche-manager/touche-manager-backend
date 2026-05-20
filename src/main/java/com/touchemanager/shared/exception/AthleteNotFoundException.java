package com.touchemanager.shared.exception;

public class AthleteNotFoundException extends RuntimeException {

    public AthleteNotFoundException(String email) {
        super("Athlete profile not found for user: " + email);
    }
}
