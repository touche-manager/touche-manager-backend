package com.touchemanager.shared.exception;

public class TournamentNotOwnedException extends RuntimeException {

    public TournamentNotOwnedException(Long tournamentId) {
        super("You do not have permission to manage tournament with ID: " + tournamentId);
    }
}
