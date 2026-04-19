package com.ruwanthi.pet_clinic.medical.dto;
import java.time.LocalDateTime;
import java.util.List;
public record PrescriptionResponse(
        Long id,
        Long petId,
        LocalDateTime prescribedAt,
        String diagnosis,
        String notes,
        Long prescribedByStaffId,
        String prescribedByStaffName,
        LocalDateTime createdAt,
        List<PrescriptionItemResponse> items
) {
}
