package com.ruwanthi.pet_clinic.medical.dto;

import com.ruwanthi.pet_clinic.medical.entity.BillItemType;

import java.math.BigDecimal;

public record PosBillItemResponse(
        Long billItemId,
        BillItemType itemType,
        Long medicineId,
        String itemName,
        String description,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
