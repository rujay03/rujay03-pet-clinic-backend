package com.ruwanthi.pet_clinic.pet.repo;

import com.ruwanthi.pet_clinic.pet.entity.PetTypeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetTypeCatalogRepository extends JpaRepository<PetTypeCatalog, Long> {

    List<PetTypeCatalog> findByActiveTrueOrderByTypeNameAsc();

    boolean existsByTypeNameIgnoreCaseAndActiveTrue(String typeName);

    boolean existsByTypeNameIgnoreCaseAndActiveTrueAndIdNot(String typeName, Long id);

    Optional<PetTypeCatalog> findByIdAndActiveTrue(Long id);

    Optional<PetTypeCatalog> findFirstByTypeNameIgnoreCase(String typeName);
}
