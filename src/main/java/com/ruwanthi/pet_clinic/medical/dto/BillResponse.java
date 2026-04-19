package com.ruwanthi.pet_clinic.medical.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillResponse(
        Long billId,
        String billNo,
        LocalDateTime billedAt,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentMethod,
        String notes,
        Long createdByStaffId,
        String createdByStaffName,
        LocalDateTime createdAt,
        List<PosBillItemResponse> items
) {
}
