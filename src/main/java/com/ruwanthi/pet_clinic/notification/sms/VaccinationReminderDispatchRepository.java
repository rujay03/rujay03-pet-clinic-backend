package com.ruwanthi.pet_clinic.notification.sms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VaccinationReminderDispatchRepository extends JpaRepository<VaccinationReminderDispatch, Long> {

    @Query("""
            select count(d) > 0
            from VaccinationReminderDispatch d
            where d.vaccination.id = :vaccinationId
              and d.reminderType = :reminderType
              and d.status = :status
            """)
    boolean existsByVaccinationIdAndReminderTypeAndStatus(
            @Param("vaccinationId") Long vaccinationId,
            @Param("reminderType") VaccinationReminderDispatch.ReminderType reminderType,
            @Param("status") VaccinationReminderDispatch.DispatchStatus status
    );
}

