package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long customerId;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeamLocationRole> roles = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "team_members",
        joinColumns = @JoinColumn(name = "team_id")
    )
    @Column(name = "customer_employee_id")
    private Set<Long> memberIds = new HashSet<>();

    // Default constructor required by JPA
    protected Team() {
    }

    public Team(String name, Long customerId) {
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

    public Set<TeamLocationRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<TeamLocationRole> roles) {
        this.roles = roles;
    }

    public void addRole(TeamLocationRole role) {
        roles.add(role);
        role.setTeam(this);
    }

    public void removeRole(TeamLocationRole role) {
        roles.remove(role);
        role.setTeam(null);
    }

    public Set<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(Set<Long> memberIds) {
        this.memberIds = memberIds;
    }

    public void addMember(Long employeeId) {
        memberIds.add(employeeId);
    }

    public void removeMember(Long employeeId) {
        memberIds.remove(employeeId);
    }
}
