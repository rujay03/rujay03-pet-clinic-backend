package com.ruwanthi.pet_clinic.owner.repo;

import com.ruwanthi.pet_clinic.owner.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    Optional<Owner> findByUserId(Long userId);

    boolean existsByContactNo(String contactNo);

    Optional<Owner> findByContactNo(String contactNo);
}

