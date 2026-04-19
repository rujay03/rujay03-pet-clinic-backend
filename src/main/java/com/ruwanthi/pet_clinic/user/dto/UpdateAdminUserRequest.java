package com.ruwanthi.pet_clinic.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAdminUserRequest(
        @NotBlank String name,
        @NotBlank String role,
        @NotBlank String status,
        String contactNo
) {
}
