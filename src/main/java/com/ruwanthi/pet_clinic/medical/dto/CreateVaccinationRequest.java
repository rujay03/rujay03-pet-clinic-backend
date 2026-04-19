package com.ruwanthi.pet_clinic.medical.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class CreateVaccinationRequest {
    @NotBlank
    private String vaccineName;
    @NotNull
    private LocalDateTime givenAt;
    private LocalDate validUntil;
    private String notes;
    public String getVaccineName() {
        return vaccineName;
    }
    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }
    public LocalDateTime getGivenAt() {
        return givenAt;
    }
    public void setGivenAt(LocalDateTime givenAt) {
        this.givenAt = givenAt;
    }
    public LocalDate getValidUntil() {
        return validUntil;
    }
    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
