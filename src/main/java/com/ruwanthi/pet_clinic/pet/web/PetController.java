package com.ruwanthi.pet_clinic.pet.web;

import com.ruwanthi.pet_clinic.pet.dto.CreatePetRequest;
import com.ruwanthi.pet_clinic.pet.dto.PetResponse;
import com.ruwanthi.pet_clinic.pet.dto.UpdatePetRequest;
import com.ruwanthi.pet_clinic.pet.service.PetImageService;
import com.ruwanthi.pet_clinic.pet.service.PetService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import com.ruwanthi.pet_clinic.pet.repo.PetTypeCatalogRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;
    private final PetImageService petImageService;
    private final UserRepository userRepository;
    private final PetTypeCatalogRepository petTypeCatalogRepository;

    public PetController(PetService petService, PetImageService petImageService,
                         UserRepository userRepository,
                         PetTypeCatalogRepository petTypeCatalogRepository) {
        this.petService = petService;
        this.petImageService = petImageService;
        this.userRepository = userRepository;
        this.petTypeCatalogRepository = petTypeCatalogRepository;
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
     * Serve a pet image by filename (no auth required so <img> tags work)
     */
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getPetImage(@PathVariable String filename) {
        try {
            Path imagePath = petImageService.getImagePath(filename);
            Resource resource = new UrlResource(imagePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new pet (multipart/form-data)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPet(
            @RequestParam("name") String name,
            @RequestParam("species") String species,
            @RequestParam(value = "breed", required = false) String breed,
            @RequestParam("sex") String sex,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            User user = getCurrentUser();

            CreatePetRequest request = new CreatePetRequest();
            request.setName(name);
            request.setSpecies(species);
            request.setBreed(breed);
            request.setSex(sex);
            if (dateOfBirth != null && !dateOfBirth.isBlank()) {
                request.setDateOfBirth(LocalDate.parse(dateOfBirth));
            }
            request.setNotes(notes);

            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                String filename = petImageService.saveImage(image);
                imageUrl = "/api/pets/images/" + filename;
            }

            PetResponse pet = petService.createPet(request, user, imageUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(pet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to upload image: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create pet"));
        }
    }

    /**
     * Update an existing pet (multipart/form-data)
     */
    @PutMapping(value = "/{petId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePet(
            @PathVariable Long petId,
            @RequestParam("name") String name,
            @RequestParam("species") String species,
            @RequestParam(value = "breed", required = false) String breed,
            @RequestParam("sex") String sex,
            @RequestParam(value = "dateOfBirth", required = false) String dateOfBirth,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "removeImage", required = false, defaultValue = "false") boolean removeImage) {
        try {
            User user = getCurrentUser();

            UpdatePetRequest request = new UpdatePetRequest();
            request.setName(name);
            request.setSpecies(species);
            request.setBreed(breed);
            request.setSex(sex);
            if (dateOfBirth != null && !dateOfBirth.isBlank()) {
                request.setDateOfBirth(LocalDate.parse(dateOfBirth));
            }
            request.setNotes(notes);

            // Handle image logic
            String newImageUrl = null; // null = keep existing
            if (removeImage) {
                newImageUrl = ""; // empty string = remove image
            } else if (image != null && !image.isEmpty()) {
                String filename = petImageService.saveImage(image);
                newImageUrl = "/api/pets/images/" + filename;
            }

            PetResponse pet = petService.updatePet(petId, request, user, newImageUrl);
            return ResponseEntity.ok(pet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to upload image: " + e.getMessage()));
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
     * Get all pets for a specific owner (doctor/admin view)
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<?> getPetsByOwner(@PathVariable Long ownerId) {
        try {
            List<PetResponse> pets = petService.getPetsByOwnerId(ownerId);
            return ResponseEntity.ok(pets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get pet details by ID for doctor/admin views
     */
    @GetMapping("/details/{petId}")
    public ResponseEntity<?> getPetDetailsForDoctor(@PathVariable Long petId) {
        try {
            PetResponse pet = petService.getPetByIdForDoctor(petId);
            return ResponseEntity.ok(pet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Create a new pet for a selected owner (doctor/admin view)
     */
    @PostMapping("/owner/{ownerId}")
    public ResponseEntity<?> createPetForOwner(
            @PathVariable Long ownerId,
            @RequestBody CreatePetRequest request
    ) {
        try {
            User user = getCurrentUser();
            PetResponse pet = petService.createPetForOwner(ownerId, request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(pet);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create pet"));
        }
    }

    /**
     * Get active pet species catalog for authenticated users
     */
    @GetMapping("/species")
    public ResponseEntity<List<String>> getPetSpeciesCatalog() {
        getCurrentUser();
        List<String> species = petTypeCatalogRepository.findByActiveTrueOrderByTypeNameAsc().stream()
                .map(type -> type.getTypeName() == null ? "" : type.getTypeName().trim())
                .filter(name -> !name.isBlank())
                .toList();
        return ResponseEntity.ok(species);
    }

    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
