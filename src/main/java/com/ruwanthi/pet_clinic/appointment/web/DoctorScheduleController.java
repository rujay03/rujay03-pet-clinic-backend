package com.ruwanthi.pet_clinic.appointment.web;

import com.ruwanthi.pet_clinic.appointment.dto.AddDoctorSlotRequest;
import com.ruwanthi.pet_clinic.appointment.schedule.ScheduleService;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.DoctorSummary;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.TimeSlotDto;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/doctors")
public class DoctorScheduleController {

    private final StaffRepository staffRepository;
    private final ScheduleService scheduleService;
    private final UserRepository userRepository;

    public DoctorScheduleController(StaffRepository staffRepository, ScheduleService scheduleService, UserRepository userRepository) {
        this.staffRepository = staffRepository;
        this.scheduleService = scheduleService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<DoctorSummary> getDoctors() {
        return staffRepository.findByActiveTrueOrderByFullNameAsc()
                .stream()
                .map(d -> new DoctorSummary(d.getId(), d.getFullName()))
                .toList();
    }

    @GetMapping("/{doctorId}/available-slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var slots = scheduleService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/my/available-slots")
    public ResponseEntity<List<TimeSlotDto>> getMyAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long excludeAppointmentId
    ) {
        Staff doctor = getCurrentDoctor();
        var slots = scheduleService.getAvailableSlots(doctor.getId(), date, excludeAppointmentId);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/my/slots")
    public ResponseEntity<List<TimeSlotDto>> getMyManagedSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Staff doctor = getCurrentDoctor();
        return ResponseEntity.ok(scheduleService.getDoctorManagedSlots(doctor.getId(), date));
    }

    @PostMapping("/my/slots")
    public ResponseEntity<?> addMyManagedSlot(@Valid @RequestBody AddDoctorSlotRequest request) {
        try {
            Staff doctor = getCurrentDoctor();
            var slots = scheduleService.addDoctorManagedSlot(doctor.getId(), request.getDate(), request.getStartTime(), request.getEndTime());
            return ResponseEntity.ok(slots);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/my/slots")
    public ResponseEntity<?> deleteMyManagedSlot(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        try {
            Staff doctor = getCurrentDoctor();
            var slots = scheduleService.removeDoctorManagedSlot(doctor.getId(), date, startTime, endTime);
            return ResponseEntity.ok(slots);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Staff getCurrentDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));
    }
}
