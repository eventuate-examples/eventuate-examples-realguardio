package io.eventuate.examples.realguardio.securitysystemservice.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "security_system")
@Access(AccessType.FIELD)
public class SecuritySystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String locationName;
    
    @Enumerated(EnumType.STRING)
    private SecuritySystemState state;

    private Long locationId;
    
    private String rejectionReason;
    
    @Version
    private Long version;

    public SecuritySystem() {
    }

    public SecuritySystem(String locationName, SecuritySystemState state) {
        this.locationName = locationName;
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    public String getLocationName() {
        return locationName;
    }

    public SecuritySystemState getState() {
        return state;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public void setState(SecuritySystemState state) {
        this.state = state;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public void disarm() {
        this.state = SecuritySystemState.DISARMED;
    }
    
    public void arm() {
        if (this.state == SecuritySystemState.ALARMED) {
            throw new IllegalStateException("Cannot arm system in ALARMED state");
        }
        this.state = SecuritySystemState.ARMED;
    }
}
