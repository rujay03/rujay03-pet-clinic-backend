package com.ruwanthi.pet_clinic.pet.entity;

import com.ruwanthi.pet_clinic.owner.entity.Owner;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pet_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60)
    private String species;

    @Column(length = 80)
    private String breed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex = Sex.UNKNOWN;

    @Column(name = "dob")
    private LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sex == null) {
            this.sex = Sex.UNKNOWN;
        }
    }

    public enum Sex {
        MALE, FEMALE, UNKNOWN
    }
}

