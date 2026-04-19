package com.ruwanthi.pet_clinic.appointment.web;
import com.ruwanthi.pet_clinic.appointment.dto.AppointmentResponse;
import com.ruwanthi.pet_clinic.appointment.dto.CreateAppointmentRequest;
import com.ruwanthi.pet_clinic.appointment.dto.DoctorAppointmentItemResponse;
import com.ruwanthi.pet_clinic.appointment.dto.DoctorAppointmentPageResponse;
import com.ruwanthi.pet_clinic.appointment.dto.UpdateAppointmentStatusRequest;
import com.ruwanthi.pet_clinic.appointment.dto.UpdateDoctorAppointmentRequest;
import com.ruwanthi.pet_clinic.appointment.service.AppointmentService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
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
    /** Get all appointments for the logged-in doctor */
    @GetMapping("/doctor/my")
    public ResponseEntity<DoctorAppointmentPageResponse> getDoctorAppointments(
            @RequestParam(defaultValue = "ALL") String tab,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getCurrentUser();
        DoctorAppointmentPageResponse response = appointmentService.getDoctorAppointments(user, tab, search, date, page, size);
        return ResponseEntity.ok(response);
    }
    /** Update the status of a doctor's appointment */
    @PatchMapping("/doctor/{id}/status")
    public ResponseEntity<?> updateDoctorAppointmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request
    ) {
        try {
            User user = getCurrentUser();
            DoctorAppointmentItemResponse response = appointmentService.updateDoctorAppointmentStatus(id, request.getStatus(), user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update appointment status"));
        }
    }

    /** Get full details of a doctor's appointment */
    @GetMapping("/doctor/{id}")
    public ResponseEntity<?> getDoctorAppointmentById(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            DoctorAppointmentItemResponse response = appointmentService.getDoctorAppointmentById(id, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to load appointment details"));
        }
    }

    /** Edit a doctor's appointment */
    @PutMapping("/doctor/{id}")
    public ResponseEntity<?> updateDoctorAppointment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDoctorAppointmentRequest request
    ) {
        try {
            User user = getCurrentUser();
            DoctorAppointmentItemResponse response = appointmentService.updateDoctorAppointment(id, request, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update appointment"));
        }
    }

    /** Delete a doctor's appointment */
    @DeleteMapping("/doctor/{id}")
    public ResponseEntity<?> deleteDoctorAppointment(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            appointmentService.deleteDoctorAppointment(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete appointment"));
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
