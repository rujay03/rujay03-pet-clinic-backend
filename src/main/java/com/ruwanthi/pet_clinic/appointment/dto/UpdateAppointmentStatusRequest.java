package com.ruwanthi.pet_clinic.appointment.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateAppointmentStatusRequest {

    @NotBlank
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

