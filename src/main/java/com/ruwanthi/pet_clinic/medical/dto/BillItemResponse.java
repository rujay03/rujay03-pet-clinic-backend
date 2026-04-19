package com.ruwanthi.pet_clinic.medical.dto;

import java.math.BigDecimal;

public record BillItemResponse(
        Long billItemId,
        Long medicineId,
        String medicineName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
