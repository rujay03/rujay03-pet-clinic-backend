package com.ruwanthi.pet_clinic.pet.repo;

import com.ruwanthi.pet_clinic.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);
}

