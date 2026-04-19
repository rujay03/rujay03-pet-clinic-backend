package com.ruwanthi.pet_clinic.appointment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAppointmentTypeRequest(
        @NotBlank String name
) {
}

