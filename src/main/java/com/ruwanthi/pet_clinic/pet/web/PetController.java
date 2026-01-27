package com.ruwanthi.pet_clinic.pet.web;

import com.ruwanthi.pet_clinic.pet.dto.CreatePetRequest;
import com.ruwanthi.pet_clinic.pet.dto.PetResponse;
import com.ruwanthi.pet_clinic.pet.dto.UpdatePetRequest;
import com.ruwanthi.pet_clinic.pet.service.PetService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final UserRepository userRepository;

    public PetController(PetService petService, UserRepository userRepository) {
        this.petService = petService;
        this.userRepository = userRepository;
    }

    /**
     * Get all pets for the authenticated owner
     */
    @GetMapping
    public ResponseEntity<List<PetResponse>> getMyPets() {
        User user = getCurrentUser();
        List<PetResponse> pets = petService.getMyPets(user);
        return ResponseEntity.ok(pets);
    }

    /**
     * Get a specific pet by ID
     */
    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPetById(@PathVariable Long petId) {
        User user = getCurrentUser();
        PetResponse pet = petService.getPetById(petId, user);
        return ResponseEntity.ok(pet);
    }

    /**
     * Create a new pet
     */
    @PostMapping
    public ResponseEntity<?> createPet(@Valid @RequestBody CreatePetRequest request) {
        try {
            User user = getCurrentUser();
            PetResponse pet = petService.createPet(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(pet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create pet"));
        }
    }

    /**
     * Update an existing pet
     */
    @PutMapping("/{petId}")
    public ResponseEntity<?> updatePet(@PathVariable Long petId,
                                       @Valid @RequestBody UpdatePetRequest request) {
        try {
            User user = getCurrentUser();
            PetResponse pet = petService.updatePet(petId, request, user);
            return ResponseEntity.ok(pet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update pet"));
        }
    }

    /**
     * Delete a pet
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<?> deletePet(@PathVariable Long petId) {
        try {
            User user = getCurrentUser();
            petService.deletePet(petId, user);
            return ResponseEntity.ok(Map.of("message", "Pet deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete pet"));
        }
    }

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

