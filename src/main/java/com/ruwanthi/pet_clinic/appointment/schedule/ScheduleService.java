package com.ruwanthi.pet_clinic.appointment.schedule;

import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentRepository;
import com.ruwanthi.pet_clinic.appointment.schedule.dto.TimeSlotDto;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final StaffRepository staffRepository;
    private final DoctorWeeklyScheduleRepository weeklyRepo;
    private final DoctorScheduleOverrideRepository overrideRepo;
    private final AppointmentRepository appointmentRepository;

    public ScheduleService(StaffRepository staffRepository,
                           DoctorWeeklyScheduleRepository weeklyRepo,
                           DoctorScheduleOverrideRepository overrideRepo,
                           AppointmentRepository appointmentRepository) {
        this.staffRepository = staffRepository;
        this.weeklyRepo = weeklyRepo;
        this.overrideRepo = overrideRepo;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getAvailableSlots(Long doctorId, LocalDate date) {
        return getAvailableSlots(doctorId, date, null);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getAvailableSlots(Long doctorId, LocalDate date, Long excludeAppointmentId) {
        Staff doctor = staffRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<TimeSlotDto> baseSlots = new ArrayList<>();

        var overrides = overrideRepo.findByDoctorIdAndOverrideDate(doctor.getId(), date);
        if (!overrides.isEmpty()) {
            // Overrides fully replace weekly schedule
            baseSlots = overrides.stream()
                    .filter(DoctorScheduleOverride::getAvailable)
                    .map(o -> new TimeSlotDto(o.getSlotStart(), o.getSlotEnd()))
                    .collect(Collectors.toList());
        } else {
            var doctorSpecific = weeklyRepo.findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), dayOfWeek);
            var globalDefaults = weeklyRepo.findByDoctorIsNullAndDayOfWeekAndActiveTrue(dayOfWeek);
            var chosen = doctorSpecific.isEmpty() ? globalDefaults : doctorSpecific;
            baseSlots = chosen.stream()
                    .map(s -> new TimeSlotDto(s.getSlotStart(), s.getSlotEnd()))
                    .collect(Collectors.toList());
        }

        if (baseSlots.isEmpty()) {
            return List.of();
        }

        // Remove already booked slots for this doctor and date
        var booked = excludeAppointmentId == null
                ? appointmentRepository.findByStaffIdAndAppointmentDateAndStatusNot(
                        doctor.getId(), date, Appointment.AppointmentStatus.CANCELLED)
                : appointmentRepository.findByStaffIdAndAppointmentDateAndStatusNotAndIdNot(
                        doctor.getId(), date, Appointment.AppointmentStatus.CANCELLED, excludeAppointmentId);
        Map<LocalTime, Appointment> bookedByStart = booked.stream()
                .collect(Collectors.toMap(Appointment::getAppointmentTime, a -> a, (a, b) -> a));

        return baseSlots.stream()
                .filter(slot -> !bookedByStart.containsKey(slot.slotStart()))
                .sorted(Comparator.comparing(TimeSlotDto::slotStart))
                .toList();
    }

    @Transactional(readOnly = true)
    public void ensureSlotAvailable(Long doctorId, LocalDate date, LocalTime slotStart) {
        ensureSlotAvailable(doctorId, date, slotStart, null);
    }

    @Transactional(readOnly = true)
    public void ensureSlotAvailable(Long doctorId, LocalDate date, LocalTime slotStart, Long excludeAppointmentId) {
        boolean available = getAvailableSlots(doctorId, date, excludeAppointmentId).stream()
                .anyMatch(slot -> slot.slotStart().equals(slotStart));
        if (!available) {
            throw new IllegalArgumentException("Selected slot is not available");
        }
    }

    @Transactional(readOnly = true)
    public List<TimeSlotDto> getDoctorManagedSlots(Long doctorId, LocalDate date) {
        staffRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        return overrideRepo.findByDoctorIdAndOverrideDateAndAvailableTrueOrderBySlotStartAsc(doctorId, date)
                .stream()
                .map(o -> new TimeSlotDto(o.getSlotStart(), o.getSlotEnd()))
                .toList();
    }

    @Transactional
    public List<TimeSlotDto> addDoctorManagedSlot(Long doctorId, LocalDate date, LocalTime slotStart, LocalTime slotEnd) {
        if (!slotEnd.isAfter(slotStart)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Staff doctor = staffRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        DoctorScheduleOverride slot = DoctorScheduleOverride.builder()
                .doctor(doctor)
                .overrideDate(date)
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .available(true)
                .build();

        try {
            overrideRepo.save(slot);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("A slot with the same start time already exists for this date");
        }

        return getDoctorManagedSlots(doctorId, date);
    }

    @Transactional
    public List<TimeSlotDto> removeDoctorManagedSlot(Long doctorId, LocalDate date, LocalTime slotStart, LocalTime slotEnd) {
        DoctorScheduleOverride slot = overrideRepo
                .findByDoctorIdAndOverrideDateAndSlotStartAndSlotEnd(doctorId, date, slotStart, slotEnd)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        overrideRepo.delete(slot);
        return getDoctorManagedSlots(doctorId, date);
    }
}
