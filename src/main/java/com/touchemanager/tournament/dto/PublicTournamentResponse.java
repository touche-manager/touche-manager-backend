package com.touchemanager.tournament.dto;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.entity.Weapon;

import java.time.LocalDate;

/** Public tournament card — no enrollment/pricing info, no auth required */
public record PublicTournamentResponse(
        Long id,
        String name,
        Weapon weapon,
        Category category,
        Gender gender,
        String location,
        LocalDate date,
        TournamentPhase phase,
        boolean isNational
) {}
