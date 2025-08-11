package io.eventuate.examples.realguardio.customerservice.domain;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "customer")
@Access(AccessType.FIELD)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String locationName;
    
    @Enumerated(EnumType.STRING)
    private CustomerState state;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<CustomerAction> actions;
    
    @Version
    private Long version;

    public Customer() {
    }

    public Customer(String locationName, CustomerState state, Set<CustomerAction> actions) {
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

    public CustomerState getState() {
        return state;
    }

    public Set<CustomerAction> getActions() {
        return actions;
    }
}
