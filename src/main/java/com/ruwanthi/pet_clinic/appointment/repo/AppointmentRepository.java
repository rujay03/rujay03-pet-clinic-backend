package com.ruwanthi.pet_clinic.appointment.repo;

import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import com.ruwanthi.pet_clinic.appointment.entity.Appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.pet p " +
           "LEFT JOIN FETCH a.staff s " +
           "WHERE a.owner.id = :ownerId " +
           "ORDER BY a.appointmentDate DESC, a.appointmentTime DESC")
    List<Appointment> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND a.owner.id = :ownerId")
    Optional<Appointment> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    List<Appointment> findByStaffIdAndAppointmentDateAndStatusNot(Long staffId, java.time.LocalDate date, AppointmentStatus status);

    Optional<Appointment> findByIdAndStaffId(Long id, Long staffId);

    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.owner o " +
           "JOIN FETCH a.pet p " +
           "LEFT JOIN FETCH a.staff s " +
           "WHERE a.staff.id = :staffId " +
           "ORDER BY a.appointmentDate ASC, a.appointmentTime ASC")
    List<Appointment> findByStaffIdWithDetailsOrderByDateAsc(@Param("staffId") Long staffId);
}
