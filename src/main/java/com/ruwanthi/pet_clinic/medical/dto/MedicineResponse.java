package com.ruwanthi.pet_clinic.medical.dto;

public record MedicineResponse(
        Long id,
        String name,
        String genericName,
        String form,
        String strength,
        Boolean isActive
) {
}
