package com.ruwanthi.pet_clinic.appointment.schedule.dto;

import java.time.LocalTime;

public record TimeSlotDto(LocalTime slotStart, LocalTime slotEnd) {
}

