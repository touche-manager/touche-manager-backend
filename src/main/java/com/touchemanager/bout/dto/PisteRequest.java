package com.touchemanager.bout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PisteRequest {

    @NotBlank(message = "Piste is required")
    @Size(max = 50, message = "Piste must be at most 50 characters")
    private String piste;
}
