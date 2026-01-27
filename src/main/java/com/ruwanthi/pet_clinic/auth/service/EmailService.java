package com.ruwanthi.pet_clinic.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service for sending OTP codes
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send OTP code to email
     */
    public void sendOtp(String email, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("splitmatesupport@hdrlabs.dev");
            message.setTo(email);
            message.setSubject("Pet Clinic - Email Verification");
            message.setText("Your OTP code is: " + otpCode +
                          "\n\nThis code will expire in 10 minutes." +
                          "\n\nIf you didn't request this code, please ignore this email." +
                          "\n\nThank you,\nPet Clinic Team");

            mailSender.send(message);

            logger.info("==========================================");
            logger.info("✅ OTP EMAIL SENT TO: {}", email);
            logger.info("OTP CODE: {}", otpCode);
            logger.info("==========================================");
        } catch (Exception e) {
            logger.error("❌ FAILED TO SEND EMAIL TO: {}", email, e);
            logger.error("Error: {}", e.getMessage());

            // Log OTP to console as fallback
            logger.info("==========================================");
            logger.info("⚠️ EMAIL FAILED - OTP CODE FOR TESTING:");
            logger.info("RECIPIENT: {}", email);
            logger.info("OTP CODE: {}", otpCode);
            logger.info("==========================================");

            // Don't throw exception to avoid breaking the flow
            // OTP is still logged for manual verification
        }
    }

    /**
     * Send welcome email after successful registration
     */
    public void sendWelcomeEmail(String email, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("splitmatesupport@hdrlabs.dev");
            message.setTo(email);
            message.setSubject("Welcome to Pet Clinic!");
            message.setText("Dear " + name + ",\n\n" +
                          "Welcome to Pet Clinic! Your account has been successfully created.\n\n" +
                          "You can now login and start using our services.\n\n" +
                          "Thank you for choosing Pet Clinic!\n\n" +
                          "Best regards,\nPet Clinic Team");

            mailSender.send(message);
            logger.info("✅ Welcome email sent to: {}", email);
        } catch (Exception e) {
            logger.error("❌ Failed to send welcome email to: {}", email, e);
        }
    }
}

