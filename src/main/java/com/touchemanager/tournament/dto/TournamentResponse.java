package com.touchemanager.tournament.dto;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Weapon;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TournamentResponse(
        Long id,
        String name,
        Weapon weapon,
        Category category,
        Gender gender,
        String location,
        LocalDate date,
        BigDecimal basePrice,
        LocalDate regularDeadline,
        LocalDate lateDeadline,
        BigDecimal currentPrice,
        String enrollmentStatus,
        boolean alreadyEnrolled,
        String enrollmentStatusLabel,
        Long enrollmentId,
        boolean wasPreviouslyPaid
) {}
