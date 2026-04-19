package com.ruwanthi.pet_clinic.owner.web;

import com.ruwanthi.pet_clinic.owner.dto.CreateOwnerRequest;
import com.ruwanthi.pet_clinic.owner.dto.OwnerListItemResponse;
import com.ruwanthi.pet_clinic.owner.entity.Owner;
import com.ruwanthi.pet_clinic.owner.repo.OwnerRepository;
import com.ruwanthi.pet_clinic.owner.service.OwnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pet-owner")
public class OwnerController {

    private final OwnerRepository ownerRepository;
    private final OwnerService ownerService;

    public OwnerController(OwnerRepository ownerRepository, OwnerService ownerService) {
        this.ownerRepository = ownerRepository;
        this.ownerService = ownerService;
    }

    @GetMapping("/list")
    public List<OwnerListItemResponse> getOwners() {
        return ownerRepository.findAllByOrderByFullNameAsc()
                .stream()
                .map(this::toOwnerListItem)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        try {
            Owner owner = ownerService.createOwner(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(toOwnerListItem(owner));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create pet owner"));
        }
    }

    private OwnerListItemResponse toOwnerListItem(Owner owner) {
        return new OwnerListItemResponse(
                owner.getId(),
                owner.getFullName(),
                owner.getUser() != null ? owner.getUser().getEmail() : null,
                owner.getAddress(),
                owner.getContactNo()
        );
    }
}
