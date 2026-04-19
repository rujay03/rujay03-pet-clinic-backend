package com.ruwanthi.pet_clinic.appointment.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AdminWeeklyScheduleResponse(
        Long scheduleId,
        Long doctorId,
        String doctorName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}

