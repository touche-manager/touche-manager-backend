package com.touchemanager.athlete.dto;

import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EliminationRound;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A bout seen from the authenticated athlete's perspective ("Mis Combates") */
public record AthleteBoutResponse(
        Long boutId,
        Long tournamentId,
        String tournamentName,
        LocalDate tournamentDate,
        BoutFormat format,
        EliminationRound eliminationRound,
        Integer pouleNumber,
        String opponentName,
        String opponentClub,
        int myScore,
        int opponentScore,
        Boolean won,            // null while the bout is unfinished
        BoutStatus status,
        String piste,
        LocalDateTime finishedAt
) {}
