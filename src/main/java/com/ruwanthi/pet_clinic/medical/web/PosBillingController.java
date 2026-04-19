package com.ruwanthi.pet_clinic.medical.web;

import com.ruwanthi.pet_clinic.medical.dto.BillResponse;
import com.ruwanthi.pet_clinic.medical.dto.CreateBillRequest;
import com.ruwanthi.pet_clinic.medical.service.PosBillingService;
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
@RequestMapping("/api/pos")
public class PosBillingController {

    private final PosBillingService posBillingService;
    private final UserRepository userRepository;

    public PosBillingController(PosBillingService posBillingService, UserRepository userRepository) {
        this.posBillingService = posBillingService;
        this.userRepository = userRepository;
    }

    @PostMapping("/bills")
    public ResponseEntity<?> createBill(@Valid @RequestBody CreateBillRequest request) {
        try {
            User user = getCurrentUser();
            BillResponse response = posBillingService.createBill(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save bill: " + e.getMessage()));
        }
    }

    @GetMapping("/bills")
    public ResponseEntity<?> listBills() {
        try {
            User user = getCurrentUser();
            List<BillResponse> response = posBillingService.listBills(user);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load bills: " + e.getMessage()));
        }
    }

    @GetMapping("/bills/{billId}")
    public ResponseEntity<?> getBillById(@PathVariable Long billId) {
        try {
            User user = getCurrentUser();
            BillResponse response = posBillingService.getBillById(billId, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load bill: " + e.getMessage()));
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
