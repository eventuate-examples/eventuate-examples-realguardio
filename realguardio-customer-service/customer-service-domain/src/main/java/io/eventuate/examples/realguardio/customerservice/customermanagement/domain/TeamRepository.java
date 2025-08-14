package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, TeamRepositoryCustom {
    /**
     * Check if an employee is a member of any team that has a specific role for a location with a specific security system.
     *
     * @param securitySystemId the ID of the security system
     * @param employeeId the ID of the employee
     * @param roleName the name of the role
     * @return the count of matching records (1 or more if authorized, 0 if not)
     */
    @Query("""
        SELECT COUNT(ce)
        FROM CustomerEmployee ce, Team t
        JOIN t.roles tlr
        JOIN Location l ON tlr.locationId = l.id
        WHERE ce.id = :employeeId
        AND t.id IN (SELECT teamId FROM CustomerEmployee ce2 JOIN ce2.teamIds teamId WHERE ce2.id = :employeeId)
        AND tlr.roleName = :roleName
        AND l.securitySystemId = :securitySystemId
        """)
    Long countEmployeeWithTeamSecuritySystemRole(@Param("securitySystemId") Long securitySystemId,
                                                @Param("employeeId") Long employeeId,
                                                @Param("roleName") String roleName);
}
