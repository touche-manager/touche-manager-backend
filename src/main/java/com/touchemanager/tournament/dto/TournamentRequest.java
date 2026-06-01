package com.touchemanager.tournament.dto;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Weapon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TournamentRequest {

    @NotBlank(message = "Tournament name is required")
    private String name;

    @NotNull(message = "Weapon is required")
    private Weapon weapon;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Tournament date must be today or in the future")
    private LocalDate date;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price must be non-negative")
    private BigDecimal basePrice;
}
