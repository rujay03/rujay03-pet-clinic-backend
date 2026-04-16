package com.ruwanthi.pet_clinic.appointment.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface DoctorWeeklyScheduleRepository extends JpaRepository<DoctorWeeklySchedule, Long> {
    List<DoctorWeeklySchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);
    List<DoctorWeeklySchedule> findByDoctorIsNullAndDayOfWeekAndActiveTrue(DayOfWeek dayOfWeek);
}

