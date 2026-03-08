package com.ruwanthi.pet_clinic.appointment.web;
import com.ruwanthi.pet_clinic.appointment.dto.AppointmentResponse;
import com.ruwanthi.pet_clinic.appointment.dto.CreateAppointmentRequest;
import com.ruwanthi.pet_clinic.appointment.service.AppointmentService;
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
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    public AppointmentController(AppointmentService appointmentService,
                                 UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }
    /** Book a new appointment */
    @PostMapping
    public ResponseEntity<?> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        try {
            User user = getCurrentUser();
            AppointmentResponse response = appointmentService.createAppointment(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create appointment"));
        }
    }
    /** Get all appointments for the logged-in owner */
    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments() {
        User user = getCurrentUser();
        List<AppointmentResponse> appointments = appointmentService.getMyAppointments(user);
        return ResponseEntity.ok(appointments);
    }
    /** Cancel an appointment */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            AppointmentResponse response = appointmentService.cancelAppointment(id, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to cancel appointment"));
        }
    }
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
