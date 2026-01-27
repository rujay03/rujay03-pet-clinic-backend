package com.ruwanthi.pet_clinic.auth.repo;

import com.ruwanthi.pet_clinic.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.email = :email")
    void deleteByEmail(@Param("email") String email);

    boolean existsByEmail(String email);
}

