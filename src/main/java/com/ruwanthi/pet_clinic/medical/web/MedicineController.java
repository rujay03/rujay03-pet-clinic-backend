package com.ruwanthi.pet_clinic.medical.web;

import com.ruwanthi.pet_clinic.medical.dto.CreateMedicineRequest;
import com.ruwanthi.pet_clinic.medical.dto.MedicineResponse;
import com.ruwanthi.pet_clinic.medical.dto.UpdateMedicineRequest;
import com.ruwanthi.pet_clinic.medical.service.MedicineService;
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
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;
    private final UserRepository userRepository;

    public MedicineController(MedicineService medicineService, UserRepository userRepository) {
        this.medicineService = medicineService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> listMedicines() {
        try {
            User user = getCurrentUser();
            List<MedicineResponse> response = medicineService.listMedicines(user);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load medicines: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createMedicine(@Valid @RequestBody CreateMedicineRequest request) {
        try {
            User user = getCurrentUser();
            MedicineResponse response = medicineService.createMedicine(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save medicine: " + e.getMessage()));
        }
    }

    @PutMapping("/{medicineId}")
    public ResponseEntity<?> updateMedicine(
            @PathVariable Long medicineId,
            @Valid @RequestBody UpdateMedicineRequest request
    ) {
        try {
            User user = getCurrentUser();
            MedicineResponse response = medicineService.updateMedicine(medicineId, request, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update medicine: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{medicineId}")
    public ResponseEntity<?> deleteMedicine(@PathVariable Long medicineId) {
        try {
            User user = getCurrentUser();
            medicineService.deleteMedicine(medicineId, user);
            return ResponseEntity.ok(Map.of("message", "Medicine deleted successfully"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete medicine: " + e.getMessage()));
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
