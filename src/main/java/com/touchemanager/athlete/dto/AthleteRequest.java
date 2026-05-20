package com.touchemanager.athlete.dto;

import com.touchemanager.athlete.entity.DominantHand;
import com.touchemanager.athlete.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AthleteRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "DNI is required")
    private String dni;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Dominant hand is required")
    private DominantHand dominantHand;

    @NotBlank(message = "Club is required")
    private String club;

    @NotBlank(message = "Province is required")
    private String province;
}
