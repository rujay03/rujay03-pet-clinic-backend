package com.ruwanthi.pet_clinic.medical.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record VaccinationResponse(
        Long id,
        Long petId,
        String vaccineName,
        LocalDateTime givenAt,
        LocalDate validUntil,
        String notes,
        Long givenByStaffId,
        String givenByStaffName,
        LocalDateTime createdAt
) {
}
