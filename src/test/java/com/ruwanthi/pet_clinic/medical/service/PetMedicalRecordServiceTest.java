package com.ruwanthi.pet_clinic.medical.service;

import com.ruwanthi.pet_clinic.auth.service.EmailService;
import com.ruwanthi.pet_clinic.medical.dto.CreateVaccinationRequest;
import com.ruwanthi.pet_clinic.medical.dto.VaccinationResponse;
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
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetMedicalRecordServiceTest {

    @Mock
    private VaccinationRepository vaccinationRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private EmailService emailService;

    private PetMedicalRecordService service;

    @BeforeEach
    void setUp() {
        service = new PetMedicalRecordService(
                vaccinationRepository,
                prescriptionRepository,
                petRepository,
                staffRepository,
                medicineRepository,
                ownerRepository,
                emailService
        );
    }

    @Test
    void createVaccinationSendsEmailToPetOwner() {
        User doctorUser = doctorUser();
        Staff doctor = Staff.builder().id(11L).fullName("Dr. Silva").user(doctorUser).build();

        User ownerUser = User.builder().id(5L).email("owner@example.com").status(UserStatus.ACTIVE).build();
        Owner owner = Owner.builder().id(7L).fullName("Sam Perera").user(ownerUser).build();
        Pet pet = Pet.builder().id(20L).name("Luna").owner(owner).build();

        LocalDateTime givenAt = LocalDateTime.of(2026, 4, 19, 10, 30);
        LocalDate validUntil = LocalDate.of(2027, 4, 19);
        CreateVaccinationRequest request = vaccinationRequest("Rabies", givenAt, validUntil, "Booster dose");

        when(staffRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
        when(petRepository.findWithOwnerAndUserById(20L)).thenReturn(Optional.of(pet));
        when(vaccinationRepository.save(any(Vaccination.class))).thenAnswer((invocation) -> {
            Vaccination saved = invocation.getArgument(0);
            saved.setId(100L);
            saved.setCreatedAt(LocalDateTime.of(2026, 4, 19, 10, 31));
            return saved;
        });
        when(emailService.sendVaccinationRecordedEmail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        VaccinationResponse response = service.createVaccination(20L, request, doctorUser);

        assertEquals(100L, response.id());
        assertEquals("Rabies", response.vaccineName());
        assertEquals(validUntil, response.validUntil());
        assertNotNull(response.createdAt());

        verify(emailService).sendVaccinationRecordedEmail(
                eq("owner@example.com"),
                eq("Sam Perera"),
                eq("Luna"),
                eq("Rabies"),
                eq(givenAt),
                eq(validUntil),
                eq("Booster dose"),
                eq("Dr. Silva")
        );
    }

    @Test
    void createVaccinationDoesNotFailWhenEmailSendingFails() {
        User doctorUser = doctorUser();
        Staff doctor = Staff.builder().id(11L).fullName("Dr. Silva").user(doctorUser).build();

        User ownerUser = User.builder().id(5L).email("owner@example.com").status(UserStatus.ACTIVE).build();
        Owner owner = Owner.builder().id(7L).fullName("Sam Perera").user(ownerUser).build();
        Pet pet = Pet.builder().id(20L).name("Luna").owner(owner).build();

        LocalDateTime givenAt = LocalDateTime.of(2026, 4, 19, 10, 30);
        CreateVaccinationRequest request = vaccinationRequest("Parvo", givenAt, null, null);

        when(staffRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
        when(petRepository.findWithOwnerAndUserById(20L)).thenReturn(Optional.of(pet));
        when(vaccinationRepository.save(any(Vaccination.class))).thenAnswer((invocation) -> {
            Vaccination saved = invocation.getArgument(0);
            saved.setId(101L);
            saved.setCreatedAt(LocalDateTime.of(2026, 4, 19, 10, 31));
            return saved;
        });
        when(emailService.sendVaccinationRecordedEmail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        VaccinationResponse response = service.createVaccination(20L, request, doctorUser);

        assertEquals(101L, response.id());
        verify(emailService).sendVaccinationRecordedEmail(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createVaccinationRejectsInvalidDateOrder() {
        User doctorUser = doctorUser();
        Staff doctor = Staff.builder().id(11L).fullName("Dr. Silva").user(doctorUser).build();
        Pet pet = Pet.builder().id(20L).name("Luna").owner(Owner.builder().id(7L).build()).build();

        LocalDateTime givenAt = LocalDateTime.of(2026, 4, 19, 10, 30);
        LocalDate invalidValidUntil = LocalDate.of(2026, 4, 18);
        CreateVaccinationRequest request = vaccinationRequest("Rabies", givenAt, invalidValidUntil, null);

        when(staffRepository.findByUserId(doctorUser.getId())).thenReturn(Optional.of(doctor));
        when(petRepository.findWithOwnerAndUserById(20L)).thenReturn(Optional.of(pet));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createVaccination(20L, request, doctorUser)
        );

        assertEquals("Valid until date cannot be before given date", ex.getMessage());
        verify(vaccinationRepository, never()).save(any());
        verify(emailService, never()).sendVaccinationRecordedEmail(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private User doctorUser() {
        return User.builder()
                .id(1L)
                .email("doctor@example.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().id(1).name("DOCTOR").build()))
                .build();
    }

    private CreateVaccinationRequest vaccinationRequest(String vaccineName,
                                                        LocalDateTime givenAt,
                                                        LocalDate validUntil,
                                                        String notes) {
        CreateVaccinationRequest request = new CreateVaccinationRequest();
        request.setVaccineName(vaccineName);
        request.setGivenAt(givenAt);
        request.setValidUntil(validUntil);
        request.setNotes(notes);
        return request;
    }
}
