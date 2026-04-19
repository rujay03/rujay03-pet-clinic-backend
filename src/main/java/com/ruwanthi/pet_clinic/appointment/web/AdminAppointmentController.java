package com.ruwanthi.pet_clinic.appointment.web;

import com.ruwanthi.pet_clinic.appointment.dto.AdminAppointmentSlotRequest;
import com.ruwanthi.pet_clinic.appointment.dto.AdminAppointmentTypeResponse;
import com.ruwanthi.pet_clinic.appointment.dto.AdminWeeklyScheduleRequest;
import com.ruwanthi.pet_clinic.appointment.dto.AdminWeeklyScheduleResponse;
import com.ruwanthi.pet_clinic.appointment.dto.CreateAppointmentTypeRequest;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.DoctorSummary;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.TimeSlotDto;
import com.ruwanthi.pet_clinic.appointment.service.AdminAppointmentService;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    private final AdminAppointmentService adminAppointmentService;
    private final UserRepository userRepository;

    public AdminAppointmentController(AdminAppointmentService adminAppointmentService, UserRepository userRepository) {
        this.adminAppointmentService = adminAppointmentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/types")
    public ResponseEntity<List<AdminAppointmentTypeResponse>> getAppointmentTypes() {
        return ResponseEntity.ok(adminAppointmentService.getAppointmentTypes(getCurrentUser()));
    }

    @PostMapping("/types")
    public ResponseEntity<?> createAppointmentType(@Valid @RequestBody CreateAppointmentTypeRequest request) {
        try {
            AdminAppointmentTypeResponse created = adminAppointmentService.createAppointmentType(getCurrentUser(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/types/{typeId}")
    public ResponseEntity<?> deleteAppointmentType(@PathVariable Long typeId) {
        try {
            adminAppointmentService.deleteAppointmentType(getCurrentUser(), typeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorSummary>> getDoctors() {
        return ResponseEntity.ok(adminAppointmentService.getDoctors(getCurrentUser()));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<TimeSlotDto>> getSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(adminAppointmentService.getSlots(getCurrentUser(), doctorId, date));
    }

    @PostMapping("/slots")
    public ResponseEntity<?> addSlot(@Valid @RequestBody AdminAppointmentSlotRequest request) {
        try {
            List<TimeSlotDto> slots = adminAppointmentService.addSlot(
                    getCurrentUser(),
                    request.doctorId(),
                    request.date(),
                    request.startTime(),
                    request.endTime()
            );
            return ResponseEntity.ok(slots);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/slots")
    public ResponseEntity<?> removeSlot(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        try {
            List<TimeSlotDto> slots = adminAppointmentService.removeSlot(
                    getCurrentUser(),
                    doctorId,
                    date,
                    startTime,
                    endTime
            );
            return ResponseEntity.ok(slots);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/weekly-schedules")
    public ResponseEntity<?> getWeeklySchedules(@RequestParam(required = false) Long doctorId) {
        try {
            return ResponseEntity.ok(adminAppointmentService.getWeeklySchedules(getCurrentUser(), doctorId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/weekly-schedules")
    public ResponseEntity<?> addWeeklySchedule(@Valid @RequestBody AdminWeeklyScheduleRequest request) {
        try {
            List<AdminWeeklyScheduleResponse> schedules = adminAppointmentService.addWeeklySchedule(
                    getCurrentUser(),
                    request.doctorId(),
                    request.dayOfWeek(),
                    request.startTime(),
                    request.endTime()
            );
            return ResponseEntity.ok(schedules);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/weekly-schedules/{scheduleId}")
    public ResponseEntity<?> deleteWeeklySchedule(@PathVariable Long scheduleId) {
        try {
            List<AdminWeeklyScheduleResponse> schedules = adminAppointmentService.deleteWeeklySchedule(
                    getCurrentUser(),
                    scheduleId
            );
            return ResponseEntity.ok(schedules);
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

