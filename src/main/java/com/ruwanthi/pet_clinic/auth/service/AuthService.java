package com.ruwanthi.pet_clinic.auth.service;

import com.ruwanthi.pet_clinic.auth.dto.PetOwnerSignupRequest;
import com.ruwanthi.pet_clinic.auth.dto.SignupRequest;
import com.ruwanthi.pet_clinic.auth.dto.StaffSignupRequest;
import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.staff.entity.Staff;
import com.ruwanthi.pet_clinic.staff.repo.StaffRepository;
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

import java.util.Arrays;
import java.util.Collections;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OwnerRepository ownerRepository;
    private final StaffRepository staffRepository;
    private final OtpService otpService;


    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       OwnerRepository ownerRepository,
                       StaffRepository staffRepository,
                       OtpService otpService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.ownerRepository = ownerRepository;
        this.staffRepository = staffRepository;
        this.otpService = otpService;
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

    /**
     * Register a pet owner with full profile information
     * Step 1: Validate and send OTP
     */
    @Transactional
    public void registerPetOwnerWithProfile(PetOwnerSignupRequest request) {
        // 1) check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // 2) check duplicate contact number
        if (ownerRepository.existsByContactNo(request.getContactNo())) {
            throw new IllegalArgumentException("Contact number already in use");
        }

        // 3) Generate and send OTP
        otpService.generateAndSendOtp(request.getEmail());

        // Note: User and profile will be created after OTP verification
    }

    /**
     * Complete pet owner registration after OTP verification
     */
    @Transactional
    public void completePetOwnerRegistration(PetOwnerSignupRequest request) {
        // 1) Verify email is verified
        if (!otpService.isEmailVerified(request.getEmail())) {
            throw new IllegalStateException("Email not verified. Please verify OTP first");
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

        user = userRepository.save(user);

        // 5) create owner profile
        Owner owner = Owner.builder()
                .user(user)
                .fullName(request.getFullName())
                .contactNo(request.getContactNo())
                .address(request.getAddress())
                .build();

        ownerRepository.save(owner);

        // 6) Delete OTP record
        otpService.deleteOtp(request.getEmail());
    }

    /**
     * Register staff (doctor/pharmacist/admin) with role selection
     * Step 1: Validate and send OTP
     */
    @Transactional
    public void registerStaff(StaffSignupRequest request) {
        // 1) check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // 2) validate role
        String roleUpper = request.getRole().toUpperCase();
        if (!Arrays.asList("DOCTOR", "PHARMACIST", "ADMIN").contains(roleUpper)) {
            throw new IllegalArgumentException("Invalid role. Must be DOCTOR, PHARMACIST, or ADMIN");
        }

        // 3) Generate and send OTP
        otpService.generateAndSendOtp(request.getEmail());

        // Note: User and profile will be created after OTP verification
    }

    /**
     * Complete staff registration after OTP verification
     */
    @Transactional
    public void completeStaffRegistration(StaffSignupRequest request) {
        // 1) Verify email is verified
        if (!otpService.isEmailVerified(request.getEmail())) {
            throw new IllegalStateException("Email not verified. Please verify OTP first");
        }

        // 2) validate role
        String roleUpper = request.getRole().toUpperCase();

        // 3) find the specified role
        Role staffRole = roleRepository.findByName(roleUpper)
                .orElseThrow(() -> new IllegalStateException(roleUpper + " role not configured"));

        // 4) hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 5) create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .roles(Collections.singleton(staffRole))
                .build();

        user = userRepository.save(user);

        // 6) create staff profile
        Staff staff = Staff.builder()
                .user(user)
                .fullName(request.getFullName())
                .contactNo(request.getContactNo())
                .active(true)
                .build();

        staffRepository.save(staff);

        // 7) Delete OTP record
        otpService.deleteOtp(request.getEmail());
    }

    public Authentication login(String email, String password) {
        // this authenticate(...) call RETURNS an Authentication object
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
                otpService.generateAndSendPasswordResetOtp(user.getEmail())
        );
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 100) {
            throw new IllegalArgumentException("Password must be between 6 and 100 characters");
        }

        otpService.verifyOtp(email, otpCode);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid password reset request"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpService.deleteOtp(email);
    }

}
