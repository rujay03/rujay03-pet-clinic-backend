package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.auth.service.EmailService;
import com.ruwanthi.pet_clinic.medical.dto.CreatePrescriptionItemRequest;
import com.ruwanthi.pet_clinic.medical.dto.CreatePrescriptionRequest;
import com.ruwanthi.pet_clinic.medical.dto.CreateVaccinationRequest;
import com.ruwanthi.pet_clinic.medical.dto.MedicineOptionResponse;
import com.ruwanthi.pet_clinic.medical.dto.PrescriptionItemResponse;
import com.ruwanthi.pet_clinic.medical.dto.PrescriptionResponse;
import com.ruwanthi.pet_clinic.medical.dto.VaccinationResponse;
import com.ruwanthi.pet_clinic.medical.entity.Medicine;
import com.ruwanthi.pet_clinic.medical.entity.Prescription;
import com.ruwanthi.pet_clinic.medical.entity.PrescriptionItem;
import com.ruwanthi.pet_clinic.medical.entity.Vaccination;
import com.ruwanthi.pet_clinic.medical.repo.MedicineRepository;
import com.ruwanthi.pet_clinic.medical.repo.PrescriptionRepository;
import com.ruwanthi.pet_clinic.medical.repo.VaccinationRepository;
import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.pet.entity.Pet;
import com.ruwanthi.pet_clinic.pet.repo.PetRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetMedicalRecordService {

    private static final Logger logger = LoggerFactory.getLogger(PetMedicalRecordService.class);

    private final VaccinationRepository vaccinationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PetRepository petRepository;
    private final StaffRepository staffRepository;
    private final MedicineRepository medicineRepository;
    private final OwnerRepository ownerRepository;
    private final EmailService emailService;

    public PetMedicalRecordService(
            VaccinationRepository vaccinationRepository,
            PrescriptionRepository prescriptionRepository,
            PetRepository petRepository,
            StaffRepository staffRepository,
            MedicineRepository medicineRepository,
            OwnerRepository ownerRepository,
            EmailService emailService
    ) {
        this.vaccinationRepository = vaccinationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.petRepository = petRepository;
        this.staffRepository = staffRepository;
        this.medicineRepository = medicineRepository;
        this.ownerRepository = ownerRepository;
        this.emailService = emailService;
    }

    @Transactional
    public VaccinationResponse createVaccination(Long petId, CreateVaccinationRequest request, User user) {
        Staff doctor = requireDoctorStaff(user);
        Pet pet = getPetWithOwnerAndUser(petId);

        if (request.getValidUntil() != null && request.getValidUntil().isBefore(request.getGivenAt().toLocalDate())) {
            throw new IllegalArgumentException("Valid until date cannot be before given date");
        }

        Vaccination vaccination = Vaccination.builder()
                .pet(pet)
                .givenByStaff(doctor)
                .vaccineName(request.getVaccineName().trim())
                .givenAt(request.getGivenAt())
                .validUntil(request.getValidUntil())
                .notes(blankToNull(request.getNotes()))
                .build();

        Vaccination savedVaccination = vaccinationRepository.save(vaccination);
        notifyOwnerVaccinationRecorded(savedVaccination, pet, doctor);
        return toVaccinationResponse(savedVaccination);
    }

    @Transactional(readOnly = true)
    public List<VaccinationResponse> listVaccinations(Long petId, User user) {
        Pet pet = getPet(petId);
        requireMedicalRecordReadAccess(user, pet);
        return vaccinationRepository.findByPetIdOrderByGivenAtDesc(petId)
                .stream()
                .map(this::toVaccinationResponse)
                .toList();
    }

    @Transactional
    public PrescriptionResponse createPrescription(Long petId, CreatePrescriptionRequest request, User user) {
        Staff doctor = requireDoctorStaff(user);
        Pet pet = getPet(petId);

        Prescription prescription = Prescription.builder()
                .pet(pet)
                .prescribedByStaff(doctor)
                .prescribedAt(request.getPrescribedAt() != null ? request.getPrescribedAt() : LocalDateTime.now())
                .diagnosis(blankToNull(request.getDiagnosis()))
                .notes(blankToNull(request.getNotes()))
                .build();

        List<PrescriptionItem> items = request.getItems()
                .stream()
                .map((item) -> buildPrescriptionItem(prescription, item))
                .toList();

        prescription.setItems(items);
        return toPrescriptionResponse(prescriptionRepository.save(prescription));
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listPrescriptions(Long petId, User user) {
        Pet pet = getPet(petId);
        requireMedicalRecordReadAccess(user, pet);
        return prescriptionRepository.findByPetIdWithItemsOrderByPrescribedAtDesc(petId)
                .stream()
                .map(this::toPrescriptionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicineOptionResponse> searchMedicines(String query, User user) {
        requireDoctorStaff(user);

        List<Medicine> medicines;
        if (query == null || query.isBlank()) {
            medicines = medicineRepository.findTop20ByIsActiveTrueOrderByNameAsc();
        } else {
            medicines = medicineRepository.findTop20ByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(query.trim());
        }

        return medicines.stream()
                .map((medicine) -> new MedicineOptionResponse(medicine.getId(), medicine.getName()))
                .toList();
    }

    private PrescriptionItem buildPrescriptionItem(Prescription prescription, CreatePrescriptionItemRequest request) {
        Medicine medicine = medicineRepository.findByIdAndIsActiveTrue(request.getMedicineId())
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found"));

        return PrescriptionItem.builder()
                .prescription(prescription)
                .medicine(medicine)
                .medicineName(medicine.getName())
                .dosage(request.getDosage().trim())
                .frequency(request.getFrequency().trim())
                .durationDays(request.getDurationDays())
                .quantity(request.getQuantity())
                .instructions(blankToNull(request.getInstructions()))
                .build();
    }

    private Pet getPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
    }

    private Pet getPetWithOwnerAndUser(Long petId) {
        return petRepository.findWithOwnerAndUserById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
    }

    private Staff requireDoctorStaff(User user) {
        boolean doctorOrAdmin = hasAnyRole(user, "DOCTOR", "ADMIN");

        if (!doctorOrAdmin) {
            throw new SecurityException("Only doctors can manage medical records");
        }

        return staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));
    }

    private void requireMedicalRecordReadAccess(User user, Pet pet) {
        if (hasAnyRole(user, "DOCTOR", "ADMIN")) {
            return;
        }

        if (hasAnyRole(user, "PETOWNER")) {
            Owner owner = ownerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new SecurityException("Owner profile not found"));

            if (pet.getOwner().getId().equals(owner.getId())) {
                return;
            }
        }

        throw new SecurityException("You don't have permission to view these medical records");
    }

    private boolean hasAnyRole(User user, String... allowedRoles) {
        return user.getRoles().stream()
                .map((role) -> role.getName().toUpperCase())
                .anyMatch((name) -> java.util.Arrays.stream(allowedRoles)
                        .anyMatch((allowedRole) -> allowedRole.equalsIgnoreCase(name)));
    }

    private VaccinationResponse toVaccinationResponse(Vaccination vaccination) {
        return new VaccinationResponse(
                vaccination.getId(),
                vaccination.getPet().getId(),
                vaccination.getVaccineName(),
                vaccination.getGivenAt(),
                vaccination.getValidUntil(),
                vaccination.getNotes(),
                vaccination.getGivenByStaff().getId(),
                vaccination.getGivenByStaff().getFullName(),
                vaccination.getCreatedAt()
        );
    }

    private PrescriptionResponse toPrescriptionResponse(Prescription prescription) {
        List<PrescriptionItemResponse> items = prescription.getItems()
                .stream()
                .map((item) -> new PrescriptionItemResponse(
                        item.getId(),
                        item.getMedicine() != null ? item.getMedicine().getId() : null,
                        item.getMedicineName(),
                        item.getDosage(),
                        item.getFrequency(),
                        item.getDurationDays(),
                        item.getQuantity(),
                        item.getInstructions()
                ))
                .toList();

        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPet().getId(),
                prescription.getPrescribedAt(),
                prescription.getDiagnosis(),
                prescription.getNotes(),
                prescription.getPrescribedByStaff().getId(),
                prescription.getPrescribedByStaff().getFullName(),
                prescription.getCreatedAt(),
                items
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void notifyOwnerVaccinationRecorded(Vaccination vaccination, Pet pet, Staff doctor) {
        String ownerEmail = pet.getOwner() != null
                && pet.getOwner().getUser() != null
                ? pet.getOwner().getUser().getEmail()
                : null;

        if (ownerEmail == null || ownerEmail.isBlank()) {
            Long ownerId = pet.getOwner() != null ? pet.getOwner().getId() : null;
            logger.warn(
                    "Vaccination {} created for pet {} but owner email is missing (ownerId={})",
                    vaccination.getId(),
                    pet.getId(),
                    ownerId
            );
            return;
        }

        logger.info(
                "Sending vaccination-created email for vaccination {} to {}",
                vaccination.getId(),
                ownerEmail
        );

        boolean sent = emailService.sendVaccinationRecordedEmail(
                ownerEmail,
                pet.getOwner().getFullName(),
                pet.getName(),
                vaccination.getVaccineName(),
                vaccination.getGivenAt(),
                vaccination.getValidUntil(),
                vaccination.getNotes(),
                doctor.getFullName()
        );

        if (!sent) {
            logger.warn("Failed to send vaccination-created email for vaccination {} to {}", vaccination.getId(), ownerEmail);
        }
    }
}
