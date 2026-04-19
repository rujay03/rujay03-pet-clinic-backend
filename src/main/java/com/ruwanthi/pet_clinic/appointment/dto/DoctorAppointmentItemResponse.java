package com.ruwanthi.pet_clinic.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorAppointmentItemResponse(
        Long id,
        Long ownerId,
        String ownerName,
        String phoneNumber,
        Long petId,
        String petName,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String appointmentType,
        String notes,
        String status,
        String statusCode
) {
}
