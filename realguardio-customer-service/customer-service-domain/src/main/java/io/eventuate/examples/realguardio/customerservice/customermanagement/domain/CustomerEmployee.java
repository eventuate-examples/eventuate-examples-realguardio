package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customer_employees")
public class CustomerEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long memberId;

    @ElementCollection
    @CollectionTable(
        name = "customer_employee_teams",
        joinColumns = @JoinColumn(name = "customer_employee_id")
    )
    @Column(name = "team_id")
    private Set<Long> teamIds = new HashSet<>();

    // Default constructor required by JPA
    protected CustomerEmployee() {
    }

    public CustomerEmployee(Long customerId, Long memberId) {
        this.customerId = customerId;
        this.memberId = memberId;
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

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Set<Long> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(Set<Long> teamIds) {
        this.teamIds = teamIds;
    }
}
