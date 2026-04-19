package com.ruwanthi.pet_clinic.medical.dto;

import java.util.List;

public record SaleResponse(
        Long medicineId,
        String medicineName,
        Integer soldQuantity,
        Integer remainingQuantity,
        List<SaleBatchDeductionResponse> deductions
) {
}

