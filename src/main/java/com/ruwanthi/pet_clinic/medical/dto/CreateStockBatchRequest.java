package com.ruwanthi.pet_clinic.medical.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateStockBatchRequest(
        @NotNull(message = "Medicine is required")
        Long medicineId,

        @NotBlank(message = "Batch number is required")
        @Size(max = 100, message = "Batch number must be 100 characters or fewer")
        String batchNo,

        LocalDate expiryDate,

        @DecimalMin(value = "0.00", message = "Purchase price must be zero or greater")
        BigDecimal purchasePrice,

        @DecimalMin(value = "0.00", message = "Unit sell price must be zero or greater")
        @NotNull(message = "Unit sell price is required")
        BigDecimal unitSellPrice,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        LocalDateTime receivedAt
) {
}
