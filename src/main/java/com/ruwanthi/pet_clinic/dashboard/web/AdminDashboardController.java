package com.ruwanthi.pet_clinic.dashboard.web;

import com.ruwanthi.pet_clinic.dashboard.dto.AdminDashboardResponse;
import com.ruwanthi.pet_clinic.dashboard.service.AdminDashboardService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/dashboard/admin", "/api/admin/dashboard"})
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final UserRepository userRepository;

    public AdminDashboardController(AdminDashboardService adminDashboardService, UserRepository userRepository) {
        this.adminDashboardService = adminDashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard() {
        try {
            User user = getCurrentUser();
            AdminDashboardResponse response = adminDashboardService.getDashboard(user);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Unauthorized");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
