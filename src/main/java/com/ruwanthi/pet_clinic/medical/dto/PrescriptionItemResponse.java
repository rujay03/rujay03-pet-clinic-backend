package com.ruwanthi.pet_clinic.medical.dto;
public record PrescriptionItemResponse(
        Long id,
        Long medicineId,
        String medicineName,
        String dosage,
        String frequency,
        Integer durationDays,
        Integer quantity,
        String instructions
) {
}
