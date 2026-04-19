package com.ruwanthi.pet_clinic.auth.dto;

import java.util.Set;

public class MeResponse {

    private String email;
    private Set<String> roles;
    private String fullName;
    private String contactNo;
    private String address;

    public MeResponse(String email, Set<String> roles, String fullName, String contactNo, String address) {
        this.email = email;
        this.roles = roles;
        this.fullName = fullName;
        this.contactNo = contactNo;
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getFullName() {
        return fullName;
    }

    public String getContactNo() {
        return contactNo;
    }

    public String getAddress() {
        return address;
    }
}
