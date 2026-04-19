package com.ruwanthi.pet_clinic.dashboard.service;

import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentRepository;
import com.ruwanthi.pet_clinic.dashboard.dto.AdminDashboardResponse;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminDashboardService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    public AdminDashboardService(UserRepository userRepository, AppointmentRepository appointmentRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(User currentUser) {
        ensureAdmin(currentUser);

        long totalUsers = userRepository.count();
        long petOwners = userRepository.countDistinctByRoles_Name("PETOWNER");
        long doctors = userRepository.countDistinctByRoles_Name("DOCTOR");
        long pharmacists = userRepository.countDistinctByRoles_Name("PHARMACIST");

        List<AdminDashboardResponse.UserItem> recentUsers = userRepository.findTop6ByOrderByCreatedAtDesc().stream()
                .map(user -> new AdminDashboardResponse.UserItem(
                        user.getId(),
                        user.getEmail(),
                        toDisplayName(user.getEmail()),
                        pickPrimaryRole(user),
                        user.getStatus().name()
                ))
                .toList();

        List<AdminDashboardResponse.AppointmentItem> todayAppointments = appointmentRepository
                .findByAppointmentDateWithDetails(LocalDate.now()).stream()
                .map(appointment -> new AdminDashboardResponse.AppointmentItem(
                        appointment.getId(),
                        appointment.getPet().getName(),
                        appointment.getOwner().getFullName(),
                        appointment.getStaff() != null ? appointment.getStaff().getFullName() : "Unassigned",
                        appointment.getAppointmentTime().format(TIME_FORMATTER),
                        appointment.getStatus().name()
                ))
                .toList();

        return new AdminDashboardResponse(
                new AdminDashboardResponse.DashboardStats(totalUsers, petOwners, doctors, pharmacists),
                recentUsers,
                todayAppointments
        );
    }

    private void ensureAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
        if (!isAdmin) {
            throw new IllegalStateException("Admin access required");
        }
    }

    private String pickPrimaryRole(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .min(Comparator.comparingInt(this::rolePriority))
                .orElse("USER");
    }

    private int rolePriority(String role) {
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> 1;
            case "DOCTOR" -> 2;
            case "PETOWNER" -> 3;
            case "PHARMACIST" -> 4;
            default -> 10;
        };
    }

    private String toDisplayName(String email) {
        if (email == null || email.isBlank()) {
            return "Unknown User";
        }
        String localPart = email.split("@")[0];
        String cleaned = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (cleaned.isBlank()) {
            return email;
        }

        return java.util.Arrays.stream(cleaned.split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + " " + right)
                .orElse(email);
    }
}

