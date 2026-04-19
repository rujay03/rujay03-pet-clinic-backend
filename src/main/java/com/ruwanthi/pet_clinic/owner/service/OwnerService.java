package com.ruwanthi.pet_clinic.owner.service;

import com.ruwanthi.pet_clinic.owner.dto.CreateOwnerRequest;
import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.entity.UserStatus;
import com.ruwanthi.pet_clinic.user.repo.RoleRepository;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class OwnerService {

    private static final String DEFAULT_OWNER_PASSWORD = "Temp@123";

    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public OwnerService(
            OwnerRepository ownerRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.ownerRepository = ownerRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Owner createOwner(CreateOwnerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (ownerRepository.existsByContactNo(request.getContactNo())) {
            throw new IllegalArgumentException("Contact number already in use");
        }

        Role ownerRole = roleRepository.findByName("PETOWNER")
                .orElseThrow(() -> new IllegalStateException("PETOWNER role not configured"));

        User user = User.builder()
                .email(request.getEmail().trim())
                .passwordHash(passwordEncoder.encode(DEFAULT_OWNER_PASSWORD))
                .status(UserStatus.ACTIVE)
                .roles(Collections.singleton(ownerRole))
                .build();

        user = userRepository.save(user);

        Owner owner = Owner.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .contactNo(request.getContactNo().trim())
                .address(request.getAddress().trim())
                .build();

        return ownerRepository.save(owner);
    }
}

