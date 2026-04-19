package com.ruwanthi.pet_clinic.medical.dto;

import java.math.BigDecimal;

public record InventoryMedicineResponse(
        Long medicineId,
        String medicineName,
        String genericName,
        String form,
        String strength,
        Boolean active,
        Integer availableQuantity,
        BigDecimal unitSellPrice
) {
}
