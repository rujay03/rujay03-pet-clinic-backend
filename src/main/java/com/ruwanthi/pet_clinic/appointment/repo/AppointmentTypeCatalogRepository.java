package com.ruwanthi.pet_clinic.appointment.repo;

import com.ruwanthi.pet_clinic.appointment.entity.AppointmentTypeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentTypeCatalogRepository extends JpaRepository<AppointmentTypeCatalog, Long> {

    List<AppointmentTypeCatalog> findByActiveTrueOrderByTypeNameAsc();

    List<AppointmentTypeCatalog> findAllByOrderByTypeNameAsc();

    boolean existsByTypeNameIgnoreCaseAndActiveTrue(String typeName);
}

