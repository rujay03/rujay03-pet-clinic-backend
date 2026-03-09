package com.ruwanthi.pet_clinic.pet.service;

import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.pet.dto.CreatePetRequest;
import com.ruwanthi.pet_clinic.pet.dto.PetResponse;
import com.ruwanthi.pet_clinic.pet.dto.UpdatePetRequest;
import com.ruwanthi.pet_clinic.pet.entity.Pet;
import com.ruwanthi.pet_clinic.pet.repo.PetRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final PetImageService petImageService;

    public PetService(PetRepository petRepository, OwnerRepository ownerRepository, PetImageService petImageService) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.petImageService = petImageService;
    }

    /**
     * Get all pets for the authenticated owner
     */
    @Transactional(readOnly = true)
    public List<PetResponse> getMyPets(User user) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));

        List<Pet> pets = petRepository.findByOwnerId(owner.getId());
        return pets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific pet by ID (verify ownership)
     */
    @Transactional(readOnly = true)
    public PetResponse getPetById(Long petId, User user) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

        // Verify ownership
        if (!pet.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to view this pet");
        }

        return mapToResponse(pet);
    }

    /**
     * Create a new pet
     */
    @Transactional
    public PetResponse createPet(CreatePetRequest request, User user, String imageUrl) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));

        // Check for duplicate pet name
        if (petRepository.existsByOwnerIdAndName(owner.getId(), request.getName())) {
            throw new IllegalArgumentException("You already have a pet with this name");
        }

        // Validate sex enum
        Pet.Sex sex;
        try {
            sex = Pet.Sex.valueOf(request.getSex().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sex value. Must be MALE, FEMALE, or UNKNOWN");
        }

        Pet pet = Pet.builder()
                .owner(owner)
                .name(request.getName())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .sex(sex)
                .dateOfBirth(request.getDateOfBirth())
                .notes(request.getNotes())
                .imageUrl(imageUrl)
                .build();

        pet = petRepository.save(pet);
        return mapToResponse(pet);
    }

    /**
     * Update an existing pet
     */
    @Transactional
    public PetResponse updatePet(Long petId, UpdatePetRequest request, User user, String newImageUrl) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

        // Verify ownership
        if (!pet.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to update this pet");
        }

        // Check for duplicate name (excluding current pet)
        if (!pet.getName().equals(request.getName()) &&
                petRepository.existsByOwnerIdAndName(owner.getId(), request.getName())) {
            throw new IllegalArgumentException("You already have a pet with this name");
        }

        // Validate sex enum
        Pet.Sex sex;
        try {
            sex = Pet.Sex.valueOf(request.getSex().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sex value. Must be MALE, FEMALE, or UNKNOWN");
        }

        // Update pet fields
        pet.setName(request.getName());
        pet.setSpecies(request.getSpecies());
        pet.setBreed(request.getBreed());
        pet.setSex(sex);
        pet.setDateOfBirth(request.getDateOfBirth());
        pet.setNotes(request.getNotes());

        // Handle image update
        if (newImageUrl != null) {
            // Delete old image if it exists
            if (pet.getImageUrl() != null) {
                String oldFilename = pet.getImageUrl().substring(pet.getImageUrl().lastIndexOf("/") + 1);
                petImageService.deleteImage(oldFilename);
            }
            // Set new image URL (empty string means remove image)
            pet.setImageUrl(newImageUrl.isEmpty() ? null : newImageUrl);
        }
        // if newImageUrl is null, keep existing image

        pet = petRepository.save(pet);
        return mapToResponse(pet);
    }

    /**
     * Delete a pet
     */
    @Transactional
    public void deletePet(Long petId, User user) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

        // Verify ownership
        if (!pet.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You don't have permission to delete this pet");
        }

        // Delete associated image file if exists
        if (pet.getImageUrl() != null) {
            String filename = pet.getImageUrl().substring(pet.getImageUrl().lastIndexOf("/") + 1);
            petImageService.deleteImage(filename);
        }

        petRepository.delete(pet);
    }

    /**
     * Map Pet entity to PetResponse DTO
     */
    private PetResponse mapToResponse(Pet pet) {
        PetResponse response = new PetResponse();
        response.setId(pet.getId());
        response.setName(pet.getName());
        response.setSpecies(pet.getSpecies());
        response.setBreed(pet.getBreed());
        response.setSex(pet.getSex().name());
        response.setDateOfBirth(pet.getDateOfBirth());
        response.setNotes(pet.getNotes());
        response.setImageUrl(pet.getImageUrl());
        response.setCreatedAt(pet.getCreatedAt());

        // Calculate age if date of birth is available
        if (pet.getDateOfBirth() != null) {
            Period period = Period.between(pet.getDateOfBirth(), LocalDate.now());
            response.setAge(period.getYears());
        }

        return response;
    }
}

