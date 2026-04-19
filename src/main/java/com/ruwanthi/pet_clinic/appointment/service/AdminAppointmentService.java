package com.ruwanthi.pet_clinic.appointment.service;

import com.ruwanthi.pet_clinic.appointment.dto.AdminAppointmentTypeResponse;
import com.ruwanthi.pet_clinic.appointment.dto.AdminWeeklyScheduleResponse;
import com.ruwanthi.pet_clinic.appointment.dto.CreateAppointmentTypeRequest;
import com.ruwanthi.pet_clinic.appointment.entity.AppointmentTypeCatalog;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentRepository;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentTypeCatalogRepository;
import com.ruwanthi.pet_clinic.appointment.schedule.DoctorWeeklySchedule;
import com.ruwanthi.pet_clinic.appointment.schedule.DoctorWeeklyScheduleRepository;
import com.ruwanthi.pet_clinic.appointment.schedule.ScheduleService;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.DoctorSummary;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.TimeSlotDto;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminAppointmentService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    private final AppointmentTypeCatalogRepository appointmentTypeCatalogRepository;
    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;
    private final ScheduleService scheduleService;
    private final DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository;

    public AdminAppointmentService(AppointmentTypeCatalogRepository appointmentTypeCatalogRepository,
                                   AppointmentRepository appointmentRepository,
                                   StaffRepository staffRepository,
                                   ScheduleService scheduleService,
                                   DoctorWeeklyScheduleRepository doctorWeeklyScheduleRepository) {
        this.appointmentTypeCatalogRepository = appointmentTypeCatalogRepository;
        this.appointmentRepository = appointmentRepository;
        this.staffRepository = staffRepository;
        this.scheduleService = scheduleService;
        this.doctorWeeklyScheduleRepository = doctorWeeklyScheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminAppointmentTypeResponse> getAppointmentTypes(User currentUser) {
        ensureAdmin(currentUser);

        Map<String, Long> countByType = appointmentRepository.countByAppointmentType().stream()
                .collect(Collectors.toMap(
                        p -> p.getAppointmentType().trim().toLowerCase(Locale.ROOT),
                        AppointmentRepository.AppointmentTypeCountProjection::getCount,
                        (a, b) -> a
                ));

        return appointmentTypeCatalogRepository.findByActiveTrueOrderByTypeNameAsc().stream()
                .map(type -> new AdminAppointmentTypeResponse(
                        type.getId(),
                        type.getTypeName(),
                        countByType.getOrDefault(type.getTypeName().trim().toLowerCase(Locale.ROOT), 0L)
                ))
                .toList();
    }

    @Transactional
    public AdminAppointmentTypeResponse createAppointmentType(User currentUser, CreateAppointmentTypeRequest request) {
        ensureAdmin(currentUser);

        String name = request.name().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Type name is required");
        }

        if (appointmentTypeCatalogRepository.existsByTypeNameIgnoreCaseAndActiveTrue(name)) {
            throw new IllegalArgumentException("Appointment type already exists");
        }

        AppointmentTypeCatalog saved = appointmentTypeCatalogRepository.save(
                AppointmentTypeCatalog.builder()
                        .typeName(name)
                        .active(true)
                        .build()
        );

        return new AdminAppointmentTypeResponse(saved.getId(), saved.getTypeName(), 0L);
    }

    @Transactional
    public void deleteAppointmentType(User currentUser, Long typeId) {
        ensureAdmin(currentUser);

        AppointmentTypeCatalog type = appointmentTypeCatalogRepository.findById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment type not found"));

        type.setActive(false);
        appointmentTypeCatalogRepository.save(type);
    }

    @Transactional(readOnly = true)
    public List<DoctorSummary> getDoctors(User currentUser) {
        ensureAdmin(currentUser);
        return staffRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .map(staff -> new DoctorSummary(staff.getId(), staff.getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getSlots(User currentUser, Long doctorId, LocalDate date) {
        ensureAdmin(currentUser);
        return scheduleService.getAvailableSlots(doctorId, date);
    }

    @Transactional
    public List<TimeSlotDto> addSlot(User currentUser, Long doctorId, LocalDate date, LocalTime start, LocalTime end) {
        ensureAdmin(currentUser);
        return scheduleService.addDoctorManagedSlot(doctorId, date, start, end);
    }

    @Transactional
    public List<TimeSlotDto> removeSlot(User currentUser, Long doctorId, LocalDate date, LocalTime start, LocalTime end) {
        ensureAdmin(currentUser);
        return scheduleService.removeDoctorManagedSlot(doctorId, date, start, end);
    }

    @Transactional(readOnly = true)
    public List<AdminWeeklyScheduleResponse> getWeeklySchedules(User currentUser, Long doctorId) {
        ensureAdmin(currentUser);

        List<DoctorWeeklySchedule> schedules;
        if (doctorId == null) {
            schedules = doctorWeeklyScheduleRepository.findByDoctorIsNullAndActiveTrueOrderByDayOfWeekAscSlotStartAsc();
        } else {
            staffRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
            schedules = doctorWeeklyScheduleRepository.findByDoctorIdAndActiveTrueOrderByDayOfWeekAscSlotStartAsc(doctorId);
        }

        return schedules.stream().map(this::toWeeklyResponse).toList();
    }

    @Transactional
    public List<AdminWeeklyScheduleResponse> addWeeklySchedule(
            User currentUser,
            Long doctorId,
            DayOfWeek dayOfWeek,
            LocalTime start,
            LocalTime end
    ) {
        ensureAdmin(currentUser);

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Staff doctor = null;
        List<DoctorWeeklySchedule> sameDaySchedules;
        if (doctorId != null) {
            doctor = staffRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
            sameDaySchedules = doctorWeeklyScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek);
        } else {
            sameDaySchedules = doctorWeeklyScheduleRepository.findByDoctorIsNullAndDayOfWeekAndActiveTrue(dayOfWeek);
        }

        for (DoctorWeeklySchedule existing : sameDaySchedules) {
            LocalTime existingStart = existing.getSlotStart();
            LocalTime existingEnd = existing.getSlotEnd();

            if (start.equals(existingStart) && end.equals(existingEnd)) {
                throw new IllegalArgumentException(
                        "This weekly slot already exists: " + formatRange(existingStart, existingEnd)
                );
            }

            if (isOverlapping(start, end, existingStart, existingEnd)) {
                throw new IllegalArgumentException(
                        "Time slot conflict. It overlaps with existing slot " + formatRange(existingStart, existingEnd)
                );
            }
        }

        DoctorWeeklySchedule schedule = DoctorWeeklySchedule.builder()
                .doctor(doctor)
                .dayOfWeek(dayOfWeek)
                .slotStart(start)
                .slotEnd(end)
                .active(true)
                .build();

        doctorWeeklyScheduleRepository.save(schedule);
        return getWeeklySchedules(currentUser, doctorId);
    }

    @Transactional
    public List<AdminWeeklyScheduleResponse> deleteWeeklySchedule(User currentUser, Long scheduleId) {
        ensureAdmin(currentUser);

        DoctorWeeklySchedule schedule = doctorWeeklyScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Weekly schedule not found"));

        Long doctorId = schedule.getDoctor() != null ? schedule.getDoctor().getId() : null;
        schedule.setActive(false);
        doctorWeeklyScheduleRepository.save(schedule);

        return getWeeklySchedules(currentUser, doctorId);
    }

    private AdminWeeklyScheduleResponse toWeeklyResponse(DoctorWeeklySchedule schedule) {
        Long doctorId = schedule.getDoctor() != null ? schedule.getDoctor().getId() : null;
        String doctorName = schedule.getDoctor() != null ? schedule.getDoctor().getFullName() : "Global Default";

        return new AdminWeeklyScheduleResponse(
                schedule.getId(),
                doctorId,
                doctorName,
                schedule.getDayOfWeek(),
                schedule.getSlotStart(),
                schedule.getSlotEnd(),
                Boolean.TRUE.equals(schedule.getActive())
        );
    }

    private String formatRange(LocalTime start, LocalTime end) {
        return start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT);
    }

    private boolean isOverlapping(LocalTime start, LocalTime end, LocalTime existingStart, LocalTime existingEnd) {
        return start.isBefore(existingEnd) && end.isAfter(existingStart);
    }

    private void ensureAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ADMIN"::equalsIgnoreCase);
        if (!isAdmin) {
            throw new IllegalStateException("Admin access required");
        }
    }
}
