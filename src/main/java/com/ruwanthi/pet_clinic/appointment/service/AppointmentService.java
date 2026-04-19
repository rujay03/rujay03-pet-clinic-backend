package com.ruwanthi.pet_clinic.appointment.service;

import com.ruwanthi.pet_clinic.appointment.dto.AppointmentResponse;
import com.ruwanthi.pet_clinic.appointment.dto.CreateAppointmentRequest;
import com.ruwanthi.pet_clinic.appointment.dto.DoctorAppointmentItemResponse;
import com.ruwanthi.pet_clinic.appointment.dto.DoctorAppointmentPageResponse;
import com.ruwanthi.pet_clinic.appointment.dto.UpdateDoctorAppointmentRequest;
import com.ruwanthi.pet_clinic.appointment.entity.Appointment;
import com.ruwanthi.pet_clinic.appointment.repo.AppointmentRepository;
import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.pet.entity.Pet;
import com.ruwanthi.pet_clinic.pet.repo.PetRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.appointment.schedule.ScheduleService;
import com.ruwanthi.pet_clinic.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final StaffRepository staffRepository;
    private final ScheduleService scheduleService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              OwnerRepository ownerRepository,
                              PetRepository petRepository,
                              StaffRepository staffRepository,
                              ScheduleService scheduleService) {
        this.appointmentRepository = appointmentRepository;
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.staffRepository = staffRepository;
        this.scheduleService = scheduleService;
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
        Staff doctor = staffRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        scheduleService.ensureSlotAvailable(doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime());
        Appointment appointment = Appointment.builder()
                .owner(owner)
                .pet(pet)
                .staff(doctor)
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

    @Transactional(readOnly = true)
    public DoctorAppointmentPageResponse getDoctorAppointments(
            User user,
            String tab,
            String search,
            LocalDate date,
            int page,
            int size
    ) {
        Staff doctor = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));

        String safeTab = (tab == null ? "ALL" : tab.trim().toUpperCase(Locale.ROOT));
        String safeSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase(Locale.ROOT);

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 50),
                Sort.by("appointmentDate").ascending().and(Sort.by("appointmentTime").ascending())
        );

        Specification<Appointment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("staff").get("id"), doctor.getId()));

            if (date != null) {
                predicates.add(cb.equal(root.get("appointmentDate"), date));
            }

            LocalDate today = LocalDate.now();
            switch (safeTab) {
                case "UPCOMING" -> {
                    predicates.add(cb.greaterThan(root.get("appointmentDate"), today));
                    predicates.add(root.get("status").in(
                            Appointment.AppointmentStatus.PENDING,
                            Appointment.AppointmentStatus.CONFIRMED
                    ));
                }
                case "COMPLETED" -> predicates.add(cb.equal(root.get("status"), Appointment.AppointmentStatus.COMPLETED));
                case "TODAY" -> {
                    predicates.add(cb.equal(root.get("appointmentDate"), today));
                    predicates.add(cb.notEqual(root.get("status"), Appointment.AppointmentStatus.CANCELLED));
                }
                case "ALL" -> {
                    // No extra predicates: return all doctor appointments from the database.
                }
                default -> {
                    // Fallback to ALL behavior for unknown tab values.
                }
            }

            if (safeSearch != null) {
                Join<Object, Object> ownerJoin = root.join("owner", JoinType.INNER);
                Join<Object, Object> petJoin = root.join("pet", JoinType.INNER);
                Predicate ownerMatch = cb.like(cb.lower(ownerJoin.get("fullName")), "%" + safeSearch + "%");
                Predicate petMatch = cb.like(cb.lower(petJoin.get("name")), "%" + safeSearch + "%");
                predicates.add(cb.or(ownerMatch, petMatch));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Appointment> results = appointmentRepository.findAll(spec, pageable);
        List<DoctorAppointmentItemResponse> content = results.getContent().stream()
                .map(this::mapToDoctorAppointmentResponse)
                .toList();

        return new DoctorAppointmentPageResponse(
                content,
                results.getNumber() + 1,
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages(),
                results.isFirst(),
                results.isLast()
        );
    }

    @Transactional
    public DoctorAppointmentItemResponse updateDoctorAppointmentStatus(Long appointmentId, String status, User user) {
        Staff doctor = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));

        Appointment appointment = appointmentRepository.findByIdAndStaffId(appointmentId, doctor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        Appointment.AppointmentStatus newStatus;
        try {
            newStatus = Appointment.AppointmentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid status");
        }

        Appointment.AppointmentStatus currentStatus = appointment.getStatus();
        boolean canMarkCompleted =
                currentStatus == Appointment.AppointmentStatus.PENDING
                        || currentStatus == Appointment.AppointmentStatus.CONFIRMED
                        || currentStatus == Appointment.AppointmentStatus.IN_CONSULTATION;

        if (!(canMarkCompleted && newStatus == Appointment.AppointmentStatus.COMPLETED)) {
            throw new IllegalArgumentException("Only upcoming appointments can be marked as completed");
        }

        appointment.setStatus(newStatus);
        appointmentRepository.save(appointment);
        return mapToDoctorAppointmentResponse(appointment);
    }

    @Transactional(readOnly = true)
    public DoctorAppointmentItemResponse getDoctorAppointmentById(Long appointmentId, User user) {
        Staff doctor = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));

        Appointment appointment = appointmentRepository.findByIdAndStaffId(appointmentId, doctor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        return mapToDoctorAppointmentResponse(appointment);
    }

    @Transactional
    public DoctorAppointmentItemResponse updateDoctorAppointment(Long appointmentId, UpdateDoctorAppointmentRequest request, User user) {
        Staff doctor = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));

        Appointment appointment = appointmentRepository.findByIdAndStaffId(appointmentId, doctor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        scheduleService.ensureSlotAvailable(doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime(), appointment.getId());

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setAppointmentType(request.getAppointmentType().trim());
        appointment.setNotes(request.getNotes());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            Appointment.AppointmentStatus requestedStatus;
            try {
                requestedStatus = Appointment.AppointmentStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid status");
            }

            if (requestedStatus == Appointment.AppointmentStatus.IN_CONSULTATION) {
                throw new IllegalArgumentException("Status must be Upcoming, Completed, or Cancelled");
            }

            if (requestedStatus == Appointment.AppointmentStatus.PENDING ||
                    requestedStatus == Appointment.AppointmentStatus.CONFIRMED) {
                appointment.setStatus(Appointment.AppointmentStatus.PENDING);
            } else {
                appointment.setStatus(requestedStatus);
            }
        }

        appointmentRepository.save(appointment);
        return mapToDoctorAppointmentResponse(appointment);
    }

    @Transactional
    public void deleteDoctorAppointment(Long appointmentId, User user) {
        Staff doctor = staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));

        Appointment appointment = appointmentRepository.findByIdAndStaffId(appointmentId, doctor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (appointment.getStatus() == Appointment.AppointmentStatus.IN_CONSULTATION ||
                appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot delete appointments in consultation or completed state");
        }

        appointmentRepository.delete(appointment);
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

    private DoctorAppointmentItemResponse mapToDoctorAppointmentResponse(Appointment a) {
        String uiStatus = switch (a.getStatus()) {
            case PENDING, CONFIRMED, IN_CONSULTATION -> "Upcoming";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };

        return new DoctorAppointmentItemResponse(
                a.getId(),
                a.getOwner().getId(),
                a.getOwner().getFullName(),
                a.getOwner().getContactNo(),
                a.getPet().getId(),
                a.getPet().getName(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getAppointmentType(),
                a.getNotes(),
                uiStatus,
                a.getStatus().name()
        );
    }

    private String mapStatus(Appointment.AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case CONFIRMED -> "Confirmed";
            case IN_CONSULTATION -> "In Consultation";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
}
