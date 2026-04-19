package com.ruwanthi.pet_clinic.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnerListItemResponse {
    private Long id;
    private String name;
    private String email;
    private String address;
    private String telephone;
}

