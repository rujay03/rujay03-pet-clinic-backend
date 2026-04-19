package com.ruwanthi.pet_clinic.user.dto;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String role,
        String status,
        String contactNo,
        String joinedDate
) {
}

