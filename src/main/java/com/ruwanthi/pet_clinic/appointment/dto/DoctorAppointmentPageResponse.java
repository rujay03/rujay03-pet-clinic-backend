package com.ruwanthi.pet_clinic.appointment.dto;

import java.util.List;

public record DoctorAppointmentPageResponse(
        List<DoctorAppointmentItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

