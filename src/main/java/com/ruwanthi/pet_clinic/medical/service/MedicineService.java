package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.medical.dto.CreateMedicineRequest;
import com.ruwanthi.pet_clinic.medical.dto.MedicineResponse;
import com.ruwanthi.pet_clinic.medical.dto.UpdateMedicineRequest;
import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import com.ruwanthi.pet_clinic.medical.repo.MedicineRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> listMedicines(User user) {
        requirePharmacyAccess(user);

        return medicineRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MedicineResponse createMedicine(CreateMedicineRequest request, User user) {
        requirePharmacyAccess(user);

        String normalizedName = request.name().trim();
        validateNameAvailability(normalizedName, null);

        Medicine medicine = new Medicine();
        medicine.setName(normalizedName);
        medicine.setGenericName(blankToNull(request.genericName()));
        medicine.setForm(blankToNull(request.form()));
        medicine.setStrength(blankToNull(request.strength()));
        medicine.setIsActive(request.isActive() == null || request.isActive());

        return toResponse(medicineRepository.save(medicine));
    }

    @Transactional
    public MedicineResponse updateMedicine(Long medicineId, UpdateMedicineRequest request, User user) {
        requirePharmacyAccess(user);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        String normalizedName = request.name().trim();
        validateNameAvailability(normalizedName, medicineId);

        medicine.setName(normalizedName);
        medicine.setGenericName(blankToNull(request.genericName()));
        medicine.setForm(blankToNull(request.form()));
        medicine.setStrength(blankToNull(request.strength()));
        medicine.setIsActive(request.isActive() == null || request.isActive());

        return toResponse(medicineRepository.save(medicine));
    }

    @Transactional
    public void deleteMedicine(Long medicineId, User user) {
        requirePharmacyAccess(user);

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        try {
            medicineRepository.delete(medicine);
            medicineRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Cannot delete medicine because it is used in prescriptions or stock batches");
        }
    }

    private void requirePharmacyAccess(User user) {
        boolean allowed = user.getRoles().stream().anyMatch((role) ->
                "PHARMACIST".equalsIgnoreCase(role.getName()) ||
                        "ADMIN".equalsIgnoreCase(role.getName())
        );

        if (!allowed) {
            throw new SecurityException("Only pharmacists can manage medicines");
        }
    }

    private void validateNameAvailability(String medicineName, Long medicineId) {
        boolean exists = medicineId == null
                ? medicineRepository.existsByNameIgnoreCase(medicineName)
                : medicineRepository.existsByNameIgnoreCaseAndIdNot(medicineName, medicineId);

        if (exists) {
            throw new IllegalArgumentException("A medicine with this name already exists");
        }
    }

    private MedicineResponse toResponse(Medicine medicine) {
        return new MedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getGenericName(),
                medicine.getForm(),
                medicine.getStrength(),
                medicine.getIsActive()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
