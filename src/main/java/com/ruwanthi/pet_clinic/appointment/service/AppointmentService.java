package com.ruwanthi.pet_clinic.appointment.service;
import com.ruwanthi.pet_clinic.appointment.dto.AppointmentResponse;
import com.ruwanthi.pet_clinic.appointment.dto.CreateAppointmentRequest;
import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentRepository;
import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.pet.entity.Pet;
import com.ruwanthi.pet_clinic.pet.repo.PetRepository;
import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    public AppointmentService(AppointmentRepository appointmentRepository,
                              OwnerRepository ownerRepository,
                              PetRepository petRepository) {
        this.appointmentRepository = appointmentRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, User user) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));
        if (!pet.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Pet does not belong to you");
        }
        Appointment appointment = Appointment.builder()
                .owner(owner)
                .pet(pet)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .appointmentType(request.getAppointmentType())
                .notes(request.getNotes())
                .status(Appointment.AppointmentStatus.PENDING)
                .build();
        appointment = appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(User user) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));
        return appointmentRepository.findByOwnerIdWithDetails(owner.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, User user) {
        Owner owner = ownerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Owner profile not found"));
        Appointment appointment = appointmentRepository.findByIdAndOwnerId(appointmentId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a completed appointment");
        }
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }
    private AppointmentResponse mapToResponse(Appointment a) {
        AppointmentResponse r = new AppointmentResponse();
        r.setId(a.getId());
        r.setPetId(a.getPet().getId());
        r.setPetName(a.getPet().getName());
        r.setPetSpecies(a.getPet().getSpecies());
        r.setPetBreed(a.getPet().getBreed());
        r.setDoctorName(a.getStaff() != null ? a.getStaff().getFullName() : "To be assigned");
        r.setAppointmentDate(a.getAppointmentDate());
        r.setAppointmentTime(a.getAppointmentTime());
        r.setAppointmentType(a.getAppointmentType());
        r.setStatus(mapStatus(a.getStatus()));
        r.setNotes(a.getNotes());
        return r;
    }
    private String mapStatus(Appointment.AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case CONFIRMED -> "Confirmed";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
}
