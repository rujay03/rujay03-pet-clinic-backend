package com.ruwanthi.pet_clinic.pet.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePetTypeRequest(
        @NotBlank String name
) {
}

