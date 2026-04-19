package com.ruwanthi.pet_clinic.medical.repo;

import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findAllByOrderByIdDesc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Medicine> findTop20ByIsActiveTrueOrderByNameAsc();

    List<Medicine> findTop20ByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    Optional<Medicine> findByIdAndIsActiveTrue(Long id);
}
