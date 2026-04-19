package com.ruwanthi.pet_clinic.pet.web;

import com.ruwanthi.pet_clinic.pet.dto.AdminPetTypeResponse;
import com.ruwanthi.pet_clinic.pet.dto.CreatePetTypeRequest;
import com.ruwanthi.pet_clinic.pet.service.AdminPetTypeService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pets")
public class AdminPetTypeController {

    private final AdminPetTypeService adminPetTypeService;
    private final UserRepository userRepository;

    public AdminPetTypeController(AdminPetTypeService adminPetTypeService, UserRepository userRepository) {
        this.adminPetTypeService = adminPetTypeService;
        this.userRepository = userRepository;
    }

    @GetMapping("/species")
    public ResponseEntity<List<AdminPetTypeResponse>> getPetTypes() {
        return ResponseEntity.ok(adminPetTypeService.getPetTypes(getCurrentUser()));
    }

    @PostMapping("/species")
    public ResponseEntity<?> createPetType(@Valid @RequestBody CreatePetTypeRequest request) {
        try {
            AdminPetTypeResponse created = adminPetTypeService.createPetType(getCurrentUser(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/species/{typeId}")
    public ResponseEntity<?> updatePetType(@PathVariable Long typeId, @Valid @RequestBody CreatePetTypeRequest request) {
        try {
            AdminPetTypeResponse updated = adminPetTypeService.updatePetType(getCurrentUser(), typeId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/species/{typeId}")
    public ResponseEntity<?> deletePetType(@PathVariable Long typeId) {
        try {
            adminPetTypeService.deletePetType(getCurrentUser(), typeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Authentication required");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
