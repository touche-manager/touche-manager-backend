package com.touchemanager.tournament.dto;

import com.touchemanager.athlete.entity.DocumentValidationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentValidationRequest {

    @NotNull(message = "Validation status is required")
    private DocumentValidationStatus validationStatus;

    private String reviewNotes;
}
