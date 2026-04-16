package com.ruwanthi.pet_clinic.appointment.dto;

import java.util.List;

public record DoctorDashboardResponse(
        KpiSummary kpis,
        AppointmentStatusSummary appointmentStatusSummary,
        List<AppointmentTrendPoint> appointmentTrendsLast30Days,
        List<AppointmentTrendPoint> appointmentTrendsThisYear,
        List<DistributionItem> vaccinationTypes,
        List<VisitPoint> patientVisitsThisMonth,
        List<AppointmentOverviewPoint> appointmentOverviewThisMonth,
        List<DistributionItem> topTreatmentTypes,
        List<UpcomingPatientItem> upcomingPatients
) {
    public record KpiSummary(
            long totalAppointments,
            long consultations,
            long vaccinations,
            long totalPatients
    ) {
    }

    public record AppointmentStatusSummary(
            long scheduled,
            long completed,
            long pending,
            long cancelled
    ) {
    }

    public record AppointmentTrendPoint(
            String label,
            long appointments,
            double trend
    ) {
    }

    public record DistributionItem(
            String name,
            long value
    ) {
    }

    public record VisitPoint(
            String day,
            long visits
    ) {
    }

    public record AppointmentOverviewPoint(
            String day,
            long scheduled,
            long pending,
            long cancelled
    ) {
    }

    public record UpcomingPatientItem(
            String petName,
            String ownerName
    ) {
    }
}

