package com.ruwanthi.pet_clinic.medical.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SellMedicineRequest(
        @NotNull(message = "Medicine is required")
        Long medicineId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}

