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
    
    @Version
    private Long version;

    public SecuritySystem() {
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
}
