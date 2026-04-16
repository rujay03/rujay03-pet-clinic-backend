package com.ruwanthi.pet_clinic.appointment.web;

import com.ruwanthi.pet_clinic.appointment.dto.DoctorDashboardResponse;
import com.ruwanthi.pet_clinic.appointment.service.DoctorDashboardService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/doctor")
public class DoctorDashboardController {

    private final DoctorDashboardService doctorDashboardService;
    private final UserRepository userRepository;

    public DoctorDashboardController(DoctorDashboardService doctorDashboardService, UserRepository userRepository) {
        this.doctorDashboardService = doctorDashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DoctorDashboardResponse> getDashboard() {
        User user = getCurrentUser();
        return ResponseEntity.ok(doctorDashboardService.getDashboard(user));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

