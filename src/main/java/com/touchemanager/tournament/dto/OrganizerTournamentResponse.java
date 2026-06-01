package com.touchemanager.tournament.dto;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Weapon;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrganizerTournamentResponse(
        Long id,
        String name,
        Weapon weapon,
        Category category,
        Gender gender,
        String location,
        LocalDate date,
        BigDecimal basePrice,
        long totalEnrollments,
        long paidEnrollments,
        long pendingEnrollments,
        long cancelledEnrollments
) {}
