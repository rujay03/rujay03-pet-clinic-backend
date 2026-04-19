package com.ruwanthi.pet_clinic.medical.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;
public class CreatePrescriptionRequest {
    private LocalDateTime prescribedAt;
    private String diagnosis;
    private String notes;
    @Valid
    @NotEmpty
    private List<CreatePrescriptionItemRequest> items;
    public LocalDateTime getPrescribedAt() {
        return prescribedAt;
    }
    public void setPrescribedAt(LocalDateTime prescribedAt) {
        this.prescribedAt = prescribedAt;
    }
    public String getDiagnosis() {
        return diagnosis;
    }
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public List<CreatePrescriptionItemRequest> getItems() {
        return items;
    }
    public void setItems(List<CreatePrescriptionItemRequest> items) {
        this.items = items;
    }
}
