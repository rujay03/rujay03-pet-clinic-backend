package com.ruwanthi.pet_clinic.staff.repo;

import com.ruwanthi.pet_clinic.staff.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUserId(Long userId);

    boolean existsByContactNo(String contactNo);

    List<Staff> findByActiveTrueOrderByFullNameAsc();
}
