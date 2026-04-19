package com.ruwanthi.pet_clinic.medical.repo;
import com.ruwanthi.pet_clinic.medical.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @Query("SELECT DISTINCT p FROM Prescription p LEFT JOIN FETCH p.items WHERE p.pet.id = :petId ORDER BY p.prescribedAt DESC")
    List<Prescription> findByPetIdWithItemsOrderByPrescribedAtDesc(@Param("petId") Long petId);
}
