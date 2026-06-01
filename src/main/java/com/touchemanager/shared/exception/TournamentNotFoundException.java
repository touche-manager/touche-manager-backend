package com.touchemanager.shared.exception;

public class TournamentNotFoundException extends RuntimeException {

    public TournamentNotFoundException(Long id) {
        super("Tournament not found with ID: " + id);
    }
}
