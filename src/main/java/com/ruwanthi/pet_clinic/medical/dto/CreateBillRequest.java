package com.ruwanthi.pet_clinic.medical.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateBillRequest(
        @NotEmpty(message = "At least one bill item is required")
        List<@Valid CreateBillItemRequest> items,

        @DecimalMin(value = "0.00", message = "Discount must be zero or greater")
        BigDecimal discountAmount,

        @DecimalMin(value = "0.00", message = "Tax must be zero or greater")
        BigDecimal taxAmount,

        @NotBlank(message = "Payment method is required")
        @Size(max = 40, message = "Payment method must be 40 characters or fewer")
        String paymentMethod,

        @Size(max = 255, message = "Notes must be 255 characters or fewer")
        String notes
) {
}

