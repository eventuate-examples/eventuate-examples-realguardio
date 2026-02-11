package io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the MemberRole entity.
 */
@Repository
public interface MemberRoleRepository extends JpaRepository<MemberRole, Long> {


    /**
     * Find member roles by organization ID.
     * 
     * @param organizationId the ID of the organization to search for
     * @return a list of member roles for the given organization
     */
    List<MemberRole> findByOrganizationId(Long organizationId);


    /**
     * Find member roles by member ID and organization ID.
     * 
     * @param memberId the ID of the member to search for
     * @param organizationId the ID of the organization to search for
     * @return a list of member roles for the given member and organization
     */
    List<MemberRole> findByMemberIdAndOrganizationId(Long memberId, Long organizationId);
}
