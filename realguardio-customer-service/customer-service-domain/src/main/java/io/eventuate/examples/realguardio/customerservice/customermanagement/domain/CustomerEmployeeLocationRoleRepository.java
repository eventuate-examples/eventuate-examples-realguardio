package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerEmployeeLocationRoleRepository extends JpaRepository<CustomerEmployeeLocationRole, Long> {

    /**
     * Find all role names for a specific employee at a specific location,
     * ensuring both the employee and location belong to the specified customer.
     *
     * @param userName the userName/Email of the customer employee
     * @param locationId the ID of the location
     * @return list of role names
     */
    @Query("""
        SELECT celr.roleName
        FROM CustomerEmployeeLocationRole celr
        JOIN CustomerEmployee ce ON celr.customerEmployeeId = ce.id
        JOIN Member m ON ce.memberId = m.id
        JOIN Location l ON celr.locationId = l.id
        WHERE m.emailAddress.email = :userName
        AND celr.locationId = :locationId
        """)
    List<String> findRoleNamesByUserNameAndLocationId(@Param("userName") String userName,
                                                      @Param("locationId") Long locationId);
}
