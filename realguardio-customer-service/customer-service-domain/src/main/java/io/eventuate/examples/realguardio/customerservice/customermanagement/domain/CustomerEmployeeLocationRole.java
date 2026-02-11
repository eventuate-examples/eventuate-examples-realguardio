package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_employee_location_roles")
public class CustomerEmployeeLocationRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long customerEmployeeId;

    @Column(nullable = false)
    private Long locationId;

    @Column(nullable = false)
    private String roleName;

    // Default constructor required by JPA
    protected CustomerEmployeeLocationRole() {
    }

    public CustomerEmployeeLocationRole(Long customerId, Long customerEmployeeId, Long locationId, String roleName) {
        this.customerId = customerId;
        this.customerEmployeeId = customerEmployeeId;
        this.locationId = locationId;
        this.roleName = roleName;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getCustomerEmployeeId() {
        return customerEmployeeId;
    }

    public void setCustomerEmployeeId(Long customerEmployeeId) {
        this.customerEmployeeId = customerEmployeeId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
