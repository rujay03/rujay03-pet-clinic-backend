package com.ruwanthi.pet_clinic.medical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMedicineRequest(
        @NotBlank(message = "Medicine name is required")
        @Size(max = 150, message = "Medicine name must be 150 characters or fewer")
        String name,

        @Size(max = 150, message = "Generic name must be 150 characters or fewer")
        String genericName,

        @Size(max = 60, message = "Form must be 60 characters or fewer")
        String form,

        @Size(max = 60, message = "Strength must be 60 characters or fewer")
        String strength,

        Boolean isActive
) {
}
