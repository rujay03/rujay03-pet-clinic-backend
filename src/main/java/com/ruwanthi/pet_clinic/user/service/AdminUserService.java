package com.ruwanthi.pet_clinic.user.service;

import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
import com.ruwanthi.pet_clinic.user.dto.AdminUserResponse;
import com.ruwanthi.pet_clinic.user.dto.CreateAdminUserRequest;
import com.ruwanthi.pet_clinic.user.dto.UpdateAdminUserRequest;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.entity.UserStatus;
import com.ruwanthi.pet_clinic.user.repo.RoleRepository;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OwnerRepository ownerRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            OwnerRepository ownerRepository,
                            StaffRepository staffRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.ownerRepository = ownerRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers(User currentUser) {
        ensureAdmin(currentUser);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(User currentUser, CreateAdminUserRequest request) {
        ensureAdmin(currentUser);

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String roleName = request.role().toUpperCase(Locale.ROOT);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role"));

        try {
            User user = User.builder()
                    .email(request.email().trim())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .status(UserStatus.ACTIVE)
                    .roles(new java.util.HashSet<>(Collections.singleton(role)))
                    .build();

            User saved = userRepository.save(user);
            syncProfileForRole(saved, roleName, request.name().trim(), request.contactNo());
            userRepository.flush();
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Unable to create user due to duplicate data");
        } catch (Exception ex) {
            log.error("Unexpected create user failure for email {}", request.email(), ex);
            String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            throw new IllegalArgumentException("Unable to create user: " + detail);
        }
    }

    @Transactional
    public AdminUserResponse updateUser(User currentUser, Long userId, UpdateAdminUserRequest request) {
        ensureAdmin(currentUser);

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String roleName = request.role().toUpperCase(Locale.ROOT);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role"));

        UserStatus status;
        try {
            status = UserStatus.valueOf(request.status().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status");
        }

        String updatedName = request.name().trim();
        if (updatedName.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        try {
            target.setRoles(new java.util.HashSet<>(Collections.singleton(role)));
            target.setStatus(status);

            // target is managed from findById; flush updates without merge to avoid collection replacement issues.
            syncProfileForRole(target, roleName, updatedName, request.contactNo());
            userRepository.flush();
            return toResponse(target);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Unable to update user due to duplicate data");
        } catch (Exception ex) {
            log.error("Unexpected update failure for user id {}", userId, ex);
            String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            throw new IllegalArgumentException("Unable to update user: " + detail);
        }
    }

    @Transactional
    public void deleteUser(User currentUser, Long userId) {
        ensureAdmin(currentUser);

        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            ownerRepository.findByUserId(userId).ifPresent(ownerRepository::delete);
            staffRepository.findByUserId(userId).ifPresent(staffRepository::delete);
            userRepository.delete(target);
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cannot delete user because linked records exist");
        }
    }

    private void syncProfileForRole(User user, String roleName, String displayName, String contactNo) {
        String normalizedRole = roleName.toUpperCase(Locale.ROOT);
        String cleanedContact = contactNo == null ? null : contactNo.trim();

        if ("PETOWNER".equals(normalizedRole)) {
            if (cleanedContact == null || cleanedContact.isBlank()) {
                throw new IllegalArgumentException("Contact number is required for pet owners");
            }
            ensureContactAvailable(cleanedContact, user.getId());

            Owner owner = ownerRepository.findByUserId(user.getId()).orElse(null);
            if (owner == null) {
                ownerRepository.save(Owner.builder()
                        .user(user)
                        .fullName(displayName)
                        .contactNo(cleanedContact)
                        .address(null)
                        .build());
            } else {
                owner.setFullName(displayName);
                owner.setContactNo(cleanedContact);
            }

            ownerRepository.flush();
            return;
        }

        if (cleanedContact != null && !cleanedContact.isBlank()) {
            ensureContactAvailable(cleanedContact, user.getId());
        }

        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);
        if (staff == null) {
            staffRepository.save(Staff.builder()
                    .user(user)
                    .fullName(displayName)
                    .contactNo(cleanedContact)
                    .active(true)
                    .build());
        } else {
            staff.setFullName(displayName);
            staff.setContactNo(cleanedContact);
            staff.setActive(true);
        }

        ownerRepository.flush();
        staffRepository.flush();
    }

    private void ensureContactAvailable(String contactNo, Long currentUserId) {
        Owner owner = ownerRepository.findByContactNo(contactNo).orElse(null);
        if (owner != null && !owner.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Contact number already in use");
        }

        Staff staff = staffRepository.findByContactNo(contactNo).orElse(null);
        if (staff != null && !staff.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Contact number already in use");
        }
    }

    private int rolePriority(String role) {
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> 1;
            case "DOCTOR" -> 2;
            case "PETOWNER" -> 3;
            case "PHARMACIST" -> 4;
            default -> 10;
        };
    }

    private String toDisplayName(String email) {
        if (email == null || email.isBlank()) {
            return "Unknown User";
        }
        String localPart = email.split("@")[0];
        String cleaned = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (cleaned.isBlank()) {
            return email;
        }

        return java.util.Arrays.stream(cleaned.split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + " " + right)
                .orElse(email);
    }

    private void ensureAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name));
        if (!isAdmin) {
            throw new IllegalStateException("Admin access required");
        }
    }

    private AdminUserResponse toResponse(User user) {
        Owner owner = ownerRepository.findByUserId(user.getId()).orElse(null);
        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        String name = owner != null
                ? owner.getFullName()
                : (staff != null ? staff.getFullName() : toDisplayName(user.getEmail()));
        String contactNo = owner != null
                ? owner.getContactNo()
                : (staff != null ? staff.getContactNo() : null);
        String role = user.getRoles().stream()
                .map(Role::getName)
                .min(Comparator.comparingInt(this::rolePriority))
                .orElse("PETOWNER");

        return new AdminUserResponse(
                user.getId(),
                name,
                user.getEmail(),
                role,
                user.getStatus().name(),
                contactNo,
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : null
        );
    }
}

