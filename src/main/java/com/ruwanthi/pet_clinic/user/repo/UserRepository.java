package com.ruwanthi.pet_clinic.user.repo;

import com.ruwanthi.pet_clinic.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countDistinctByRoles_Name(String roleName);

    List<User> findTop6ByOrderByCreatedAtDesc();
}
