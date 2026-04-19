package com.ruwanthi.pet_clinic.medical.dto;

import com.ruwanthi.pet_clinic.medical.entity.BillItemType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBillItemRequest(
        Long medicineId,

        @NotNull(message = "Item type is required")
        BillItemType itemType,

        @Size(max = 150, message = "Item name must be 150 characters or fewer")
        String itemName,

        @Size(max = 255, message = "Description must be 255 characters or fewer")
        String description,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", message = "Unit price must be zero or greater")
        BigDecimal unitPrice
) {
    @AssertTrue(message = "Medicine is required for PRODUCT items")
    public boolean isProductMedicineValid() {
        return itemType != BillItemType.PRODUCT || medicineId != null;
    }

    @AssertTrue(message = "Item name is required for SERVICE items")
    public boolean isServiceNameValid() {
        return itemType != BillItemType.SERVICE || (itemName != null && !itemName.isBlank());
    }
}
