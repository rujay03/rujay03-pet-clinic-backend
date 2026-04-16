package com.ruwanthi.pet_clinic.auth.web;

import com.ruwanthi.pet_clinic.auth.dto.ForgotPasswordRequest;
import com.ruwanthi.pet_clinic.auth.dto.LoginRequest;
import com.ruwanthi.pet_clinic.auth.dto.MeResponse;
import com.ruwanthi.pet_clinic.auth.dto.PetOwnerSignupRequest;
import com.ruwanthi.pet_clinic.auth.dto.ResendOtpRequest;
import com.ruwanthi.pet_clinic.auth.dto.ResetPasswordRequest;
import com.ruwanthi.pet_clinic.auth.dto.SignupRequest;
import com.ruwanthi.pet_clinic.auth.dto.StaffSignupRequest;
import com.ruwanthi.pet_clinic.auth.dto.VerifyOtpRequest;
import com.ruwanthi.pet_clinic.auth.service.AuthService;
import com.ruwanthi.pet_clinic.auth.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String OTP_MAIL_FAILURE_MSG = "Unable to send OTP email";

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;
    private final OtpService otpService;

    public AuthController(AuthService authService,
                          SecurityContextRepository securityContextRepository,
                          OtpService otpService) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
        this.otpService = otpService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        authService.registerPetOwner(request);
        return ResponseEntity.ok("Signup successful");
    }

    /**
     * Pet owner signup with full profile information
     * Sends OTP to email for verification
     */
    @PostMapping("/signup/petowner")
    public ResponseEntity<?> signupPetOwner(@Valid @RequestBody PetOwnerSignupRequest request) {
        try {
            authService.registerPetOwnerWithProfile(request);
            return ResponseEntity.ok("OTP sent to your email. Please verify to complete registration.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            if (isOtpMailTransportFailure(e)) {
                return ResponseEntity.ok("OTP generated. Email delivery is unavailable right now; check server logs for the OTP code.");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Staff signup with role selection (doctor/pharmacist/admin)
     * Sends OTP to email for verification
     */
    @PostMapping("/signup/staff")
    public ResponseEntity<?> signupStaff(@Valid @RequestBody StaffSignupRequest request) {
        try {
            authService.registerStaff(request);
            return ResponseEntity.ok("OTP sent to your email. Please verify to complete registration.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            if (isOtpMailTransportFailure(e)) {
                return ResponseEntity.ok("OTP generated. Email delivery is unavailable right now; check server logs for the OTP code.");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Verify OTP code
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            otpService.verifyOtp(request.getEmail(), request.getOtpCode());
            return ResponseEntity.ok("Email verified successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Resend OTP code
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        try {
            otpService.generateAndSendOtp(request.getEmail());
            return ResponseEntity.ok("OTP resent to your email");
        } catch (IllegalStateException e) {
            if (isOtpMailTransportFailure(e)) {
                return ResponseEntity.ok("OTP regenerated. Email delivery is unavailable right now; check server logs for the OTP code.");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Complete pet owner registration after OTP verification
     */
    @PostMapping("/complete-signup/petowner")
    public ResponseEntity<?> completePetOwnerSignup(@Valid @RequestBody PetOwnerSignupRequest request) {
        try {
            authService.completePetOwnerRegistration(request);
            return ResponseEntity.ok("Registration completed successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Complete staff registration after OTP verification
     */
    @PostMapping("/complete-signup/staff")
    public ResponseEntity<?> completeStaffSignup(@Valid @RequestBody StaffSignupRequest request) {
        try {
            authService.completeStaffRegistration(request);
            return ResponseEntity.ok("Registration completed successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // We'll use this after login is implemented
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String email = auth.getName();
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith("ROLE_"))   // keep only real roles
                .collect(Collectors.toSet());


        return ResponseEntity.ok(new MeResponse(email, roles));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {

        try {
            // 1) authenticate using AuthService
            Authentication authentication = authService.login(
                    request.getEmail(),
                    request.getPassword()
            );

            // 2) create a SecurityContext and set the Authentication
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 3) save context to session via SecurityContextRepository
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            // 4) ensure session is created (should already happen, but safe)
            httpRequest.getSession(true);

            return ResponseEntity.ok("Login successful");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(401).body("Invalid email or password");
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(401).body("Account is disabled");
        } catch (org.springframework.security.authentication.LockedException e) {
            return ResponseEntity.status(401).body("Account is locked");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred during login");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getEmail());
        // Always return success to avoid leaking whether an account exists.
        return ResponseEntity.ok("If an account exists for that email, an OTP has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword());
            return ResponseEntity.ok("Password reset successful. Please login with your new password.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean isOtpMailTransportFailure(Exception e) {
        return e.getMessage() != null && e.getMessage().contains(OTP_MAIL_FAILURE_MSG);
    }




}
