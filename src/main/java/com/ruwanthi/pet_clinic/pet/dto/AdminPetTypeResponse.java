package com.ruwanthi.pet_clinic.pet.dto;

public record AdminPetTypeResponse(
        Long id,
        String name,
        long petCount
) {
}

