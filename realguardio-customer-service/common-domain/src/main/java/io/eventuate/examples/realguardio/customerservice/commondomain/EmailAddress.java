package io.eventuate.examples.realguardio.customerservice.commondomain;

import jakarta.persistence.Embeddable;

import java.util.regex.Pattern;

@Embeddable
public record EmailAddress(String email) {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public EmailAddress {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        email = email.toLowerCase().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }
    
    @Override
    public String toString() {
        return email;
    }
}