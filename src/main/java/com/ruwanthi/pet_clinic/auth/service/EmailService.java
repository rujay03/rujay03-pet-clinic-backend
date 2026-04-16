package com.ruwanthi.pet_clinic.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service for sending OTP codes
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mockEnabled;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress,
                        @Value("${app.mail.mock-enabled:false}") boolean mockEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mockEnabled = mockEnabled;
    }

    /**
     * Send OTP code to email
     */
    public void sendOtp(String email, String otpCode) {
        if (mockEnabled) {
            logger.warn("Mail mock mode is enabled. OTP for {} is {}", email, otpCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("Pet Clinic - Email Verification");
            message.setText("Your OTP code is: " + otpCode +
                          "\n\nThis code will expire in 10 minutes." +
                          "\n\nIf you didn't request this code, please ignore this email." +
                          "\n\nThank you,\nPet Clinic Team");

            mailSender.send(message);
            logger.info("OTP email sent to: {}", email);
        } catch (Exception e) {
            // Keep signup flow alive even when SMTP is misconfigured/unavailable.
            logger.warn("Failed to send OTP email to {}. Continuing signup flow. OTP: {}", email, otpCode, e);
        }
    }

    /**
     * Send welcome email after successful registration
     */
    public void sendWelcomeEmail(String email, String name) {
        if (mockEnabled) {
            logger.info("Mail mock mode is enabled. Welcome email skipped for {}", email);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("Welcome to Pet Clinic!");
            message.setText("Dear " + name + ",\n\n" +
                          "Welcome to Pet Clinic! Your account has been successfully created.\n\n" +
                          "You can now login and start using our services.\n\n" +
                          "Thank you for choosing Pet Clinic!\n\n" +
                          "Best regards,\nPet Clinic Team");

            mailSender.send(message);
            logger.info("Welcome email sent to: {}", email);
        } catch (Exception e) {
            logger.warn("Failed to send welcome email to {}. Continuing.", email, e);
        }
    }

    /**
     * Send OTP code for password reset
     */
    public void sendPasswordResetOtp(String email, String otpCode) {
        if (mockEnabled) {
            logger.warn("Mail mock mode is enabled. Password reset OTP for {} is {}", email, otpCode);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("Pet Clinic - Password Reset OTP");
            message.setText("Your password reset OTP is: " + otpCode +
                    "\n\nThis code will expire in 10 minutes." +
                    "\n\nIf you did not request a password reset, please ignore this email." +
                    "\n\nThank you,\nPet Clinic Team");

            mailSender.send(message);
            logger.info("Password reset OTP email sent to: {}", email);
        } catch (Exception e) {
            logger.warn("Failed to send password reset OTP email to {}. Continuing flow. OTP: {}", email, otpCode, e);
        }
    }
}
