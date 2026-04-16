package com.ruwanthi.pet_clinic.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorAppointmentItemResponse(
        Long id,
        String ownerName,
        String phoneNumber,
        String petName,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String status,
        String statusCode
) {
}

