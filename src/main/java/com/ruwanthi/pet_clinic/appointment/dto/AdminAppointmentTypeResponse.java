package com.ruwanthi.pet_clinic.appointment.dto;

public record AdminAppointmentTypeResponse(
        Long id,
        String name,
        long appointmentCount
) {
}

