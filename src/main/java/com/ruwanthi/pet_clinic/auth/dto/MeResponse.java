package com.ruwanthi.pet_clinic.auth.dto;

import java.util.Set;

public class MeResponse {

    private String email;
    private Set<String> roles;

    public MeResponse(String email, Set<String> roles) {
        this.email = email;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
