package io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonName;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private PersonName name;

    @Embedded
    private EmailAddress emailAddress;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberRole> roles = new HashSet<>();

    // Default constructor required by ∆JPA
    protected Member() {
    }

    public Member(PersonName name, EmailAddress emailAddress) {
        this.name = name;
        this.emailAddress = emailAddress;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public PersonName getName() {
        return name;
    }

    public void setName(PersonName name) {
        this.name = name;
    }

    public EmailAddress getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(EmailAddress emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Set<MemberRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<MemberRole> roles) {
        this.roles = roles;
    }

    public void addRole(MemberRole role) {
        roles.add(role);
        role.setMember(this);
    }

    public void removeRole(MemberRole role) {
        roles.remove(role);
        role.setMember(null);
    }
}
