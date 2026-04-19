package com.ruwanthi.pet_clinic.medical.web;

import com.ruwanthi.pet_clinic.medical.dto.CreateStockBatchRequest;
import com.ruwanthi.pet_clinic.medical.dto.InventoryBatchResponse;
import com.ruwanthi.pet_clinic.medical.dto.InventoryMedicineResponse;
import com.ruwanthi.pet_clinic.medical.dto.SaleResponse;
import com.ruwanthi.pet_clinic.medical.dto.SellMedicineRequest;
import com.ruwanthi.pet_clinic.medical.service.InventoryService;
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
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    public InventoryController(InventoryService inventoryService, UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> listInventory() {
        try {
            User user = getCurrentUser();
            List<InventoryMedicineResponse> response = inventoryService.listInventory(user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load inventory: " + e.getMessage()));
        }
    }

    @GetMapping("/{medicineId}/batches")
    public ResponseEntity<?> listBatches(@PathVariable Long medicineId) {
        try {
            User user = getCurrentUser();
            List<InventoryBatchResponse> response = inventoryService.listBatchesByMedicine(medicineId, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load stock batches: " + e.getMessage()));
        }
    }

    @PostMapping("/batches")
    public ResponseEntity<?> addStockBatch(@Valid @RequestBody CreateStockBatchRequest request) {
        try {
            User user = getCurrentUser();
            InventoryBatchResponse response = inventoryService.addStockBatch(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save stock batch: " + e.getMessage()));
        }
    }

    @PostMapping("/sales")
    public ResponseEntity<?> sellMedicine(@Valid @RequestBody SellMedicineRequest request) {
        try {
            User user = getCurrentUser();
            SaleResponse response = inventoryService.sellMedicine(request, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to complete sale: " + e.getMessage()));
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
