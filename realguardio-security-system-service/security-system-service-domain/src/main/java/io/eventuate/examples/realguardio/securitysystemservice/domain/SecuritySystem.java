package io.eventuate.examples.realguardio.securitysystemservice.domain;

import jakarta.persistence.*;
import java.util.Set;

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
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<SecuritySystemAction> actions;
    
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

    public SecuritySystem(String locationName, SecuritySystemState state, Set<SecuritySystemAction> actions) {
        this.locationName = locationName;
        this.state = state;
        this.actions = actions;
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

    public Set<SecuritySystemAction> getActions() {
        return actions;
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
