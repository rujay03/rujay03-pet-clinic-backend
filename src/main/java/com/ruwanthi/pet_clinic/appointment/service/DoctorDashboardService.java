package com.ruwanthi.pet_clinic.appointment.service;

import com.ruwanthi.pet_clinic.appointment.dto.DoctorDashboardResponse;
import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DoctorDashboardService {

    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;

    public DoctorDashboardService(AppointmentRepository appointmentRepository, StaffRepository staffRepository) {
        this.appointmentRepository = appointmentRepository;
        this.staffRepository = staffRepository;
    }

    @Transactional(readOnly = true)
    public DoctorDashboardResponse getDashboard(User user) {
        Staff doctor = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));

        List<Appointment> appointments = appointmentRepository.findByStaffIdWithDetailsOrderByDateAsc(doctor.getId());

        long totalAppointments = appointments.size();
        long consultations = appointments.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .count();
        long vaccinations = appointments.stream()
                .filter(a -> isVaccination(a.getAppointmentType()))
                .count();
        long totalPatients = appointments.stream()
                .map(a -> a.getPet().getId())
                .filter(id -> id != null)
                .distinct()
                .count();

        long scheduled = appointments.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.CONFIRMED
                        || a.getStatus() == Appointment.AppointmentStatus.IN_CONSULTATION
                        || a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .count();
        long pending = appointments.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.PENDING)
                .count();
        long cancelled = appointments.stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.CANCELLED)
                .count();

        return new DoctorDashboardResponse(
                new DoctorDashboardResponse.KpiSummary(totalAppointments, consultations, vaccinations, totalPatients),
                new DoctorDashboardResponse.AppointmentStatusSummary(scheduled, consultations, pending, cancelled),
                buildLast30DayTrends(appointments),
                buildYearTrends(appointments),
                buildVaccinationDistribution(appointments),
                buildPatientVisitsThisMonth(appointments),
                buildAppointmentOverviewThisMonth(appointments),
                buildTopTreatmentTypes(appointments),
                buildUpcomingPatients(appointments)
        );
    }

    private List<DoctorDashboardResponse.AppointmentTrendPoint> buildLast30DayTrends(List<Appointment> appointments) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);

        Map<LocalDate, Long> perDay = appointments.stream()
                .filter(a -> !a.getAppointmentDate().isBefore(start) && !a.getAppointmentDate().isAfter(today))
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate, Collectors.counting()));

        List<DoctorDashboardResponse.AppointmentTrendPoint> points = new ArrayList<>();
        List<Long> rolling = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            long count = perDay.getOrDefault(day, 0L);
            rolling.add(count);
            if (rolling.size() > 7) {
                rolling.remove(0);
            }
            double avg = rolling.stream().mapToLong(Long::longValue).average().orElse(0.0);
            points.add(new DoctorDashboardResponse.AppointmentTrendPoint(
                    String.valueOf(day.getDayOfMonth()),
                    count,
                    Math.round(avg * 100.0) / 100.0
            ));
        }

        return points;
    }

    private List<DoctorDashboardResponse.AppointmentTrendPoint> buildYearTrends(List<Appointment> appointments) {
        int year = LocalDate.now().getYear();
        Map<Month, Long> perMonth = appointments.stream()
                .filter(a -> a.getAppointmentDate().getYear() == year)
                .collect(Collectors.groupingBy(a -> a.getAppointmentDate().getMonth(), Collectors.counting()));

        List<DoctorDashboardResponse.AppointmentTrendPoint> points = new ArrayList<>();
        List<Long> rolling = new ArrayList<>();

        for (Month month : Month.values()) {
            long count = perMonth.getOrDefault(month, 0L);
            rolling.add(count);
            if (rolling.size() > 3) {
                rolling.remove(0);
            }
            double avg = rolling.stream().mapToLong(Long::longValue).average().orElse(0.0);
            points.add(new DoctorDashboardResponse.AppointmentTrendPoint(
                    month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    count,
                    Math.round(avg * 100.0) / 100.0
            ));
        }

        return points;
    }

    private List<DoctorDashboardResponse.DistributionItem> buildVaccinationDistribution(List<Appointment> appointments) {
        Map<String, Long> grouped = appointments.stream()
                .filter(a -> isVaccination(a.getAppointmentType()))
                .map(a -> normalizeVaccinationName(a.getAppointmentType()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new DoctorDashboardResponse.DistributionItem(e.getKey(), e.getValue()))
                .toList();
    }

    private List<DoctorDashboardResponse.VisitPoint> buildPatientVisitsThisMonth(List<Appointment> appointments) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        Month month = now.getMonth();

        Map<Integer, Long> counts = appointments.stream()
                .filter(a -> a.getAppointmentDate().getYear() == year && a.getAppointmentDate().getMonth() == month)
                .filter(a -> a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(a -> a.getAppointmentDate().getDayOfMonth(), Collectors.counting()));

        int daysInMonth = now.lengthOfMonth();
        List<DoctorDashboardResponse.VisitPoint> points = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            points.add(new DoctorDashboardResponse.VisitPoint(String.valueOf(day), counts.getOrDefault(day, 0L)));
        }
        return points;
    }

    private List<DoctorDashboardResponse.AppointmentOverviewPoint> buildAppointmentOverviewThisMonth(List<Appointment> appointments) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        Month month = now.getMonth();

        Map<Integer, long[]> buckets = new HashMap<>();
        for (Appointment a : appointments) {
            LocalDate date = a.getAppointmentDate();
            if (date.getYear() != year || date.getMonth() != month) {
                continue;
            }
            long[] stat = buckets.computeIfAbsent(date.getDayOfMonth(), key -> new long[3]);
            if (a.getStatus() == Appointment.AppointmentStatus.CANCELLED) {
                stat[2]++;
            } else if (a.getStatus() == Appointment.AppointmentStatus.PENDING) {
                stat[1]++;
            } else {
                stat[0]++;
            }
        }

        int daysInMonth = now.lengthOfMonth();
        List<DoctorDashboardResponse.AppointmentOverviewPoint> points = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            long[] stat = buckets.getOrDefault(day, new long[]{0, 0, 0});
            points.add(new DoctorDashboardResponse.AppointmentOverviewPoint(
                    String.valueOf(day),
                    stat[0],
                    stat[1],
                    stat[2]
            ));
        }

        return points;
    }

    private List<DoctorDashboardResponse.DistributionItem> buildTopTreatmentTypes(List<Appointment> appointments) {
        Map<String, Long> counts = appointments.stream()
                .map(Appointment::getAppointmentType)
                .filter(type -> type != null && !type.isBlank() && !isVaccination(type))
                .collect(Collectors.groupingBy(String::trim, Collectors.counting()));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return List.of();
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    long pct = Math.round((entry.getValue() * 100.0) / total);
                    return new DoctorDashboardResponse.DistributionItem(entry.getKey(), pct);
                })
                .toList();
    }

    private List<DoctorDashboardResponse.UpcomingPatientItem> buildUpcomingPatients(List<Appointment> appointments) {
        LocalDate today = LocalDate.now();
        return appointments.stream()
                .filter(a -> !a.getAppointmentDate().isBefore(today))
                .filter(a -> a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(Appointment::getAppointmentDate)
                        .thenComparing(Appointment::getAppointmentTime))
                .limit(4)
                .map(a -> new DoctorDashboardResponse.UpcomingPatientItem(
                        a.getPet().getName(),
                        a.getOwner().getFullName()
                ))
                .toList();
    }

    private boolean isVaccination(String appointmentType) {
        if (appointmentType == null || appointmentType.isBlank()) {
            return false;
        }
        String normalized = appointmentType.toLowerCase(Locale.ROOT);
        return normalized.contains("vacc")
                || normalized.contains("rabies")
                || normalized.contains("distemper")
                || normalized.contains("parvo");
    }

    private String normalizeVaccinationName(String appointmentType) {
        String normalized = appointmentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("rabies")) {
            return "Rabies";
        }
        if (normalized.contains("distemper")) {
            return "Distemper";
        }
        if (normalized.contains("parvo")) {
            return "Parvovirus";
        }
        return "Other Vaccines";
    }
}
