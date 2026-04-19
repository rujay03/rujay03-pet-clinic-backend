package com.ruwanthi.pet_clinic.medical.dto;

import java.time.LocalDate;

public record SaleBatchDeductionResponse(
        Long batchId,
        String batchNo,
        LocalDate expiryDate,
        Integer quantityDeducted,
        Integer batchRemainingQuantity
) {
}

