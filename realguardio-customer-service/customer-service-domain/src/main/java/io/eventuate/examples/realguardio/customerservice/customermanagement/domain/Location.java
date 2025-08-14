package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationAlreadyHasSecuritySystemException;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Reference to the Customer ID
    @Column(nullable = false)
    private Long customerId;

    private Long securitySystemId;

    // Default constructor required by JPA
    protected Location() {
    }

    public Location(String name, Long customerId) {
        this.name = name;
        this.customerId = customerId;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void addSecuritySystem(Long securitySystemId) {
        if (this.securitySystemId != null) {
            throw new LocationAlreadyHasSecuritySystemException();
        }
        this.securitySystemId = securitySystemId;
    }

    public Long getSecuritySystemId() {
        return securitySystemId;
    }
}
