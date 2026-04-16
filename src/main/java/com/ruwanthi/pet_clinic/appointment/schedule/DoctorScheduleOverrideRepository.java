package com.ruwanthi.pet_clinic.appointment.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleOverrideRepository extends JpaRepository<DoctorScheduleOverride, Long> {
    List<DoctorScheduleOverride> findByDoctorIdAndOverrideDate(Long doctorId, LocalDate date);

    List<DoctorScheduleOverride> findByDoctorIdAndOverrideDateAndAvailableTrueOrderBySlotStartAsc(Long doctorId, LocalDate date);

    Optional<DoctorScheduleOverride> findByDoctorIdAndOverrideDateAndSlotStartAndSlotEnd(
            Long doctorId,
            LocalDate date,
            LocalTime slotStart,
            LocalTime slotEnd
    );
}
