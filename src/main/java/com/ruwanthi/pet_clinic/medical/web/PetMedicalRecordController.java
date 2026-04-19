package com.ruwanthi.pet_clinic.medical.web;

import com.ruwanthi.pet_clinic.medical.dto.CreatePrescriptionRequest;
import com.ruwanthi.pet_clinic.medical.dto.CreateVaccinationRequest;
import com.ruwanthi.pet_clinic.medical.dto.MedicineOptionResponse;
import com.ruwanthi.pet_clinic.medical.dto.PrescriptionResponse;
import com.ruwanthi.pet_clinic.medical.dto.VaccinationResponse;
import com.ruwanthi.pet_clinic.medical.service.PetMedicalRecordService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets/{petId}")
public class PetMedicalRecordController {

    private final PetMedicalRecordService medicalRecordService;
    private final UserRepository userRepository;

    public PetMedicalRecordController(PetMedicalRecordService medicalRecordService, UserRepository userRepository) {
        this.medicalRecordService = medicalRecordService;
        this.userRepository = userRepository;
    }

    @GetMapping("/vaccinations")
    public ResponseEntity<?> listVaccinations(@PathVariable Long petId) {
        try {
            User user = getCurrentUser();
            List<VaccinationResponse> response = medicalRecordService.listVaccinations(petId, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load vaccinations: " + e.getMessage()));
        }
    }

    @PostMapping("/vaccinations")
    public ResponseEntity<?> createVaccination(@PathVariable Long petId, @Valid @RequestBody CreateVaccinationRequest request) {
        try {
            User user = getCurrentUser();
            VaccinationResponse response = medicalRecordService.createVaccination(petId, request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save vaccination: " + e.getMessage()));
        }
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<?> listPrescriptions(@PathVariable Long petId) {
        try {
            User user = getCurrentUser();
            List<PrescriptionResponse> response = medicalRecordService.listPrescriptions(petId, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load prescriptions: " + e.getMessage()));
        }
    }

    @PostMapping("/prescriptions")
    public ResponseEntity<?> createPrescription(@PathVariable Long petId, @Valid @RequestBody CreatePrescriptionRequest request) {
        try {
            User user = getCurrentUser();
            PrescriptionResponse response = medicalRecordService.createPrescription(petId, request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save prescription: " + e.getMessage()));
        }
    }

    @GetMapping("/medicine-search")
    public ResponseEntity<?> searchMedicines(@PathVariable Long petId, @org.springframework.web.bind.annotation.RequestParam(required = false) String query) {
        try {
            User user = getCurrentUser();
            List<MedicineOptionResponse> response = medicalRecordService.searchMedicines(query, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load medicines: " + e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

