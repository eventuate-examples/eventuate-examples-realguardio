package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamLocationRoleRepository extends JpaRepository<TeamLocationRole, Long> {

    /**
     * Find all role names for teams that a user is a member of at a specific location.
     *
     * @param userName the userName/Email of the customer employee
     * @param locationId the ID of the location
     * @return list of role names from team memberships
     */
    @Query("""
        SELECT DISTINCT tlr.roleName
        FROM TeamLocationRole tlr
        JOIN tlr.team t
        JOIN t.memberIds tm
        JOIN CustomerEmployee ce ON ce.id = tm
        JOIN Member m ON ce.memberId = m.id
        WHERE m.emailAddress.email = :userName
        AND tlr.locationId = :locationId
        """)
    List<String> findTeamRolesByUserNameAndLocationId(@Param("userName") String userName,
                                                      @Param("locationId") Long locationId);
}
