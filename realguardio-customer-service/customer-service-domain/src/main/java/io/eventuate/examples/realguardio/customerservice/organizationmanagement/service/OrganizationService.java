package io.eventuate.examples.realguardio.customerservice.organizationmanagement.service;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.MemberRole;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.MemberRoleRepository;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service class for managing Organization entities.
 */
@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;

    @Autowired
    public OrganizationService(OrganizationRepository organizationRepository,
                              MemberRepository memberRepository,
                              MemberRoleRepository memberRoleRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.memberRoleRepository = memberRoleRepository;
    }


    /**
     * Create a new organization with the given name.
     *
     * @param name the name of the organization to create
     * @return the created organization
     */
    public Organization createOrganization(String name) {
        Organization organization = new Organization(name);
        return organizationRepository.save(organization);
    }


    /**
     * Find organizations by name.
     *
     * @param name the name to search for
     * @return a list of organizations with the given name
     */
    public List<Organization> findOrganizationsByName(String name) {
        return organizationRepository.findByName(name);
    }

    /**
     * Assign a role to a member in an organization.
     *
     * @param organizationId the ID of the organization
     * @param memberId the ID of the member
     * @param roleName the name of the role to assign
     * @return the created member role
     */
    public MemberRole assignRole(Long organizationId, Long memberId, String roleName) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + organizationId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + memberId));

        MemberRole role = new MemberRole(roleName, LocalDate.now(), null);
        role.setMember(member);
        role.setOrganization(organization);

        member.addRole(role);
        organization.addMemberRole(role);

        return memberRoleRepository.save(role);
    }

    /**
     * Find member roles by organization ID.
     *
     * @param organizationId the ID of the organization to search for
     * @return a list of member roles for the given organization
     */
    public List<MemberRole> findRolesByOrganizationId(Long organizationId) {
        // Check if organization exists
        if (!organizationRepository.existsById(organizationId)) {
            throw new IllegalArgumentException("Organization not found with ID: " + organizationId);
        }
        return memberRoleRepository.findByOrganizationId(organizationId);
    }

    /**
     * Find member roles by member ID and organization ID.
     *
     * @param memberId the ID of the member to search for
     * @param organizationId the ID of the organization to search for
     * @return a list of member roles for the given member and organization
     */
    public List<MemberRole> findRolesByMemberIdAndOrganizationId(Long memberId, Long organizationId) {
        // Check if member exists
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("Member not found with ID: " + memberId);
        }
        // Check if organization exists
        if (!organizationRepository.existsById(organizationId)) {
            throw new IllegalArgumentException("Organization not found with ID: " + organizationId);
        }
        return memberRoleRepository.findByMemberIdAndOrganizationId(memberId, organizationId);
    }
}
