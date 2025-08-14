package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerEmployeeLocationRoleRepository extends JpaRepository<CustomerEmployeeLocationRole, Long> {
    // Add custom query methods if needed

    /**
     * Check if an employee has a specific role for a location that has a specific security system.
     *
     * @param securitySystemId the ID of the security system
     * @param employeeId the ID of the employee
     * @param roleName the name of the role
     * @return the count of matching records (1 if authorized, 0 if not)
     */
    @Query("""
        SELECT COUNT(celr)
        FROM CustomerEmployeeLocationRole celr, Location l
        WHERE celr.locationId = l.id
        AND celr.customerEmployeeId = :employeeId
        AND celr.roleName = :roleName
        AND l.securitySystemId = :securitySystemId
        """)
    Long countEmployeeWithSecuritySystemLocationRole(@Param("securitySystemId") Long securitySystemId,
                                                    @Param("employeeId") Long employeeId,
                                                    @Param("roleName") String roleName);
    
    /**
     * Find all role names for a specific employee at a specific location,
     * ensuring both the employee and location belong to the specified customer.
     *
     * @param customerId the ID of the customer
     * @param employeeId the ID of the employee
     * @param locationId the ID of the location
     * @return list of role names
     */
    @Query("""
        SELECT celr.roleName
        FROM CustomerEmployeeLocationRole celr
        JOIN CustomerEmployee ce ON celr.customerEmployeeId = ce.id
        JOIN Location l ON celr.locationId = l.id
        WHERE ce.customerId = :customerId
        AND l.customerId = :customerId
        AND celr.customerEmployeeId = :employeeId
        AND celr.locationId = :locationId
        """)
    List<String> findRoleNamesByCustomerIdAndEmployeeIdAndLocationId(@Param("customerId") Long customerId,
                                                                     @Param("employeeId") Long employeeId,
                                                                     @Param("locationId") Long locationId);
}
