package com.ruwanthi.pet_clinic.auth.service;

import com.ruwanthi.pet_clinic.auth.dto.SignupRequest;
import com.ruwanthi.pet_clinic.user.entity.Role;
import com.ruwanthi.pet_clinic.user.entity.User;
import com.ruwanthi.pet_clinic.user.entity.UserStatus;
import com.ruwanthi.pet_clinic.user.repo.RoleRepository;
import com.ruwanthi.pet_clinic.user.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.Collections;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }



    @Transactional
    public void registerPetOwner(SignupRequest request) {
        // 1) check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // 2) find PETOWNER role
        Role petOwnerRole = roleRepository.findByName("PETOWNER")
                .orElseThrow(() -> new IllegalStateException("PETOWNER role not configured"));

        // 3) hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 4) create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .roles(Collections.singleton(petOwnerRole))
                .build();

        userRepository.save(user);
    }

    public Authentication login(String email, String password) {
        // this authenticate(...) call RETURNS an Authentication object
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
    }



}
