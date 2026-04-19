package com.ruwanthi.pet_clinic.dashboard.dto;

import java.util.List;

public record AdminDashboardResponse(
        DashboardStats stats,
        List<UserItem> recentUsers,
        List<AppointmentItem> todayAppointments
) {

    public record DashboardStats(
            long totalUsers,
            long petOwners,
            long doctors,
            long pharmacyStaff
    ) {
    }

    public record UserItem(
            Long id,
            String email,
            String displayName,
            String role,
            String status
    ) {
    }

    public record AppointmentItem(
            Long id,
            String petName,
            String ownerName,
            String doctorName,
            String time,
            String status
    ) {
    }
}

