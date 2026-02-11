package com.realguardio.endtoendtests.dto;

public record EmailAddress(String email) {
    
    public EmailAddress {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        email = email.toLowerCase().trim();
    }
    
    @Override
    public String toString() {
        return email;
    }
}