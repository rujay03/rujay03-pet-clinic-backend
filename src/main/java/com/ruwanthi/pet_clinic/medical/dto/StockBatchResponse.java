package com.ruwanthi.pet_clinic.medical.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StockBatchResponse(
        Long id,
        Long medicineId,
        String medicineName,
        String batchNo,
        LocalDate expiryDate,
        BigDecimal purchasePrice,
        Integer quantityAvailable,
        LocalDateTime receivedAt
) {
}
