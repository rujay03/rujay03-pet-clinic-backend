package com.ruwanthi.pet_clinic.appointment.repo;

import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.pet p " +
           "LEFT JOIN FETCH a.staff s " +
           "WHERE a.owner.id = :ownerId " +
           "ORDER BY a.appointmentDate DESC, a.appointmentTime DESC")
    List<Appointment> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND a.owner.id = :ownerId")
    Optional<Appointment> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);
}


