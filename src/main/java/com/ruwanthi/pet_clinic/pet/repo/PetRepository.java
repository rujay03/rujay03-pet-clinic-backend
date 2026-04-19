package com.ruwanthi.pet_clinic.pet.repo;

import com.ruwanthi.pet_clinic.pet.entity.Pet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {

    interface SpeciesCountProjection {
        String getSpeciesKey();
        String getSpeciesName();
        Long getCount();
    }

    List<Pet> findByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);

    @Query(value = """
            SELECT LOWER(TRIM(species)) AS speciesKey,
                   MIN(TRIM(species)) AS speciesName,
                   COUNT(*) AS count
            FROM pet
            WHERE species IS NOT NULL
              AND TRIM(species) <> ''
            GROUP BY LOWER(TRIM(species))
            """, nativeQuery = true)
    List<SpeciesCountProjection> countBySpeciesNormalized();

    @Query(value = """
            SELECT COUNT(*)
            FROM pet
            WHERE species IS NOT NULL
              AND LOWER(TRIM(species)) = LOWER(TRIM(:speciesName))
            """, nativeQuery = true)
    long countBySpeciesKey(@Param("speciesName") String speciesName);

    @Modifying
    @Query(value = """
            UPDATE pet
            SET species = :newSpeciesName
            WHERE species IS NOT NULL
              AND LOWER(TRIM(species)) = LOWER(TRIM(:oldSpeciesName))
            """, nativeQuery = true)
    int renameSpecies(@Param("oldSpeciesName") String oldSpeciesName, @Param("newSpeciesName") String newSpeciesName);

    @EntityGraph(attributePaths = {"owner", "owner.user"})
    java.util.Optional<Pet> findWithOwnerAndUserById(Long id);
}
