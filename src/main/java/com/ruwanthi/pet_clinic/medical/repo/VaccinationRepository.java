package com.ruwanthi.pet_clinic.medical.repo;

import com.ruwanthi.pet_clinic.medical.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    List<Vaccination> findByPetIdOrderByGivenAtDesc(Long petId);

    @Query("""
            select v.id as vaccinationId,
                   u.email as ownerEmail,
                   p.name as petName,
                   v.vaccineName as vaccineName,
                   v.validUntil as validUntil
            from Vaccination v
            join v.pet p
            join p.owner o
            join o.user u
            where v.validUntil = :targetDate
              and u.email is not null
              and u.email <> ''
            """)
    List<VaccinationReminderCandidateProjection> findReminderCandidatesByValidUntil(@Param("targetDate") LocalDate targetDate);

    interface VaccinationReminderCandidateProjection {
        Long getVaccinationId();

        String getOwnerEmail();

        String getPetName();

        String getVaccineName();

        LocalDate getValidUntil();
    }
}
