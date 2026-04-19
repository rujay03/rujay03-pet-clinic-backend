package com.ruwanthi.pet_clinic.pet.service;

import com.ruwanthi.pet_clinic.pet.dto.AdminPetTypeResponse;
import com.ruwanthi.pet_clinic.pet.dto.CreatePetTypeRequest;
import com.ruwanthi.pet_clinic.pet.entity.PetTypeCatalog;
import com.ruwanthi.pet_clinic.pet.repo.PetRepository;
import com.ruwanthi.pet_clinic.pet.repo.PetTypeCatalogRepository;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminPetTypeService {

    private final PetTypeCatalogRepository petTypeCatalogRepository;
    private final PetRepository petRepository;

    public AdminPetTypeService(PetTypeCatalogRepository petTypeCatalogRepository, PetRepository petRepository) {
        this.petTypeCatalogRepository = petTypeCatalogRepository;
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPetTypeResponse> getPetTypes(User currentUser) {
        ensureAdmin(currentUser);

        Map<String, Long> countBySpecies = petRepository.countBySpeciesNormalized().stream()
                .collect(Collectors.toMap(
                        PetRepository.SpeciesCountProjection::getSpeciesKey,
                        item -> item.getCount() == null ? 0L : item.getCount(),
                        Long::sum
                ));

        return petTypeCatalogRepository.findByActiveTrueOrderByTypeNameAsc().stream()
                .map(type -> new AdminPetTypeResponse(
                        type.getId(),
                        type.getTypeName(),
                        countBySpecies.getOrDefault(type.getTypeName().trim().toLowerCase(Locale.ROOT), 0L)
                ))
                .toList();
    }

    @Transactional
    public AdminPetTypeResponse createPetType(User currentUser, CreatePetTypeRequest request) {
        ensureAdmin(currentUser);

        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Pet species name is required");
        }

        if (petTypeCatalogRepository.existsByTypeNameIgnoreCaseAndActiveTrue(name)) {
            throw new IllegalArgumentException("Pet species already exists");
        }

        PetTypeCatalog saved = petTypeCatalogRepository.findFirstByTypeNameIgnoreCase(name)
                .map(existing -> {
                    existing.setTypeName(name);
                    existing.setActive(true);
                    return petTypeCatalogRepository.save(existing);
                })
                .orElseGet(() -> petTypeCatalogRepository.save(
                        PetTypeCatalog.builder()
                                .typeName(name)
                                .active(true)
                                .build()
                ));

        return new AdminPetTypeResponse(saved.getId(), saved.getTypeName(), 0L);
    }

    @Transactional
    public AdminPetTypeResponse updatePetType(User currentUser, Long typeId, CreatePetTypeRequest request) {
        ensureAdmin(currentUser);

        PetTypeCatalog existing = petTypeCatalogRepository.findByIdAndActiveTrue(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Pet species not found"));

        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Pet species name is required");
        }

        if (petTypeCatalogRepository.existsByTypeNameIgnoreCaseAndActiveTrueAndIdNot(name, typeId)) {
            throw new IllegalArgumentException("Pet species already exists");
        }

        String previousName = existing.getTypeName();
        existing.setTypeName(name);
        PetTypeCatalog saved = petTypeCatalogRepository.save(existing);

        if (!previousName.equalsIgnoreCase(name)) {
            petRepository.renameSpecies(previousName, name);
        }

        long count = petRepository.countBySpeciesKey(name);
        return new AdminPetTypeResponse(saved.getId(), saved.getTypeName(), count);
    }

    @Transactional
    public void deletePetType(User currentUser, Long typeId) {
        ensureAdmin(currentUser);

        PetTypeCatalog existing = petTypeCatalogRepository.findByIdAndActiveTrue(typeId)
                .orElseThrow(() -> new IllegalArgumentException("Pet species not found"));

        long usageCount = petRepository.countBySpeciesKey(existing.getTypeName());
        if (usageCount > 0) {
            throw new IllegalArgumentException("Cannot delete pet species that is assigned to pets");
        }

        existing.setActive(false);
        petTypeCatalogRepository.save(existing);
    }

    private void ensureAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ADMIN"::equalsIgnoreCase);

        if (!isAdmin) {
            throw new IllegalStateException("Admin access required");
        }
    }
}
