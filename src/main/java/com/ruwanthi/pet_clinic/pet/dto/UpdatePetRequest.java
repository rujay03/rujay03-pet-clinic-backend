package com.ruwanthi.pet_clinic.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class UpdatePetRequest {

    @NotBlank(message = "Pet name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Species is required")
    @Size(max = 60, message = "Species must not exceed 60 characters")
    private String species;

    @Size(max = 80, message = "Breed must not exceed 80 characters")
    private String breed;

    @NotNull(message = "Sex is required")
    private String sex; // MALE, FEMALE, UNKNOWN

    private LocalDate dateOfBirth;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

