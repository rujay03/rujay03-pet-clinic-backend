package com.ruwanthi.pet_clinic.auth.service;

import com.ruwanthi.pet_clinic.auth.entity.OtpVerification;
import com.ruwanthi.pet_clinic.auth.repo.OtpVerificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    public OtpService(OtpVerificationRepository otpRepository,
                      EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    /**
     * Generate and send OTP to email
     */
    @Transactional
    public void generateAndSendOtp(String email) {
        // Generate 6-digit OTP
        String otpCode = generateOtpCode();

        // Delete existing OTP for this email if any
        otpRepository.deleteByEmail(email);
        entityManager.flush(); // Force delete to complete before insert

        // Create new OTP record
        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .attempts(0)
                .build();

        otpRepository.save(otp);

        // Never block signup on SMTP transport problems.
        try {
            emailService.sendOtp(email, otpCode);
        } catch (Exception ex) {
            logger.warn("OTP mail failed for {}. Continuing signup flow. OTP: {}", email, otpCode, ex);
        }
    }

    /**
     * Generate and send OTP to email for password reset
     */
    @Transactional
    public void generateAndSendPasswordResetOtp(String email) {
        String otpCode = generateOtpCode();

        otpRepository.deleteByEmail(email);
        entityManager.flush();

        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .attempts(0)
                .build();

        otpRepository.save(otp);

        try {
            emailService.sendPasswordResetOtp(email, otpCode);
        } catch (Exception ex) {
            logger.warn("Password reset OTP mail failed for {}. Continuing flow. OTP: {}", email, otpCode, ex);
        }
    }

    /**
     * Verify OTP code
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        OtpVerification otp = otpRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found for this email"));

        // Check if already verified
        if (otp.isVerified()) {
            throw new IllegalStateException("OTP already verified");
        }

        // Check if expired
        if (otp.isExpired()) {
            throw new IllegalStateException("OTP has expired. Please request a new one");
        }

        // Check max attempts
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Maximum verification attempts exceeded. Please request a new OTP");
        }

        // Increment attempts
        otp.incrementAttempts();
        otpRepository.save(otp);

        // Verify code
        if (!otp.getOtpCode().equals(otpCode)) {
            int remainingAttempts = MAX_ATTEMPTS - otp.getAttempts();
            throw new IllegalArgumentException("Invalid OTP code. " + remainingAttempts + " attempts remaining");
        }

        // Mark as verified
        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    /**
     * Check if email is verified
     */
    public boolean isEmailVerified(String email) {
        return otpRepository.findByEmail(email)
                .map(OtpVerification::isVerified)
                .orElse(false);
    }

    /**
     * Delete OTP record after successful registration
     */
    @Transactional
    public void deleteOtp(String email) {
        otpRepository.deleteByEmail(email);
    }

    /**
     * Generate random 6-digit OTP
     */
    private String generateOtpCode() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
