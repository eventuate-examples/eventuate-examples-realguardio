package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerEmployeeRepository extends JpaRepository<CustomerEmployee, Long>, CustomerEmployeeRepositoryCustom {
    
    Optional<CustomerEmployee> findByMemberIdAndCustomerId(Long memberId, Long customerId);

    /**
     * Check if an employee is authorized to disarm a security system.
     * An employee is authorized if they have the "SecuritySystemDisarmer" role
     * and are associated with the customer that owns the security system.
     *
     * @param securitySystemId the ID of the security system
     * @param employeeId the ID of the employee
     * @return the count of matching records (1 if authorized, 0 if not)
     */
    @Query("""
        SELECT COUNT(mr)
        FROM MemberRole mr, CustomerEmployee ce, Location l
        WHERE mr.member.id = ce.memberId
        AND mr.name = :roleName
        AND ce.memberId = :employeeId
        AND l.customerId = ce.customerId
        AND l.securitySystemId = :securitySystemId
        """)
    Long countEmployeeWithSecuritySystemRole(@Param("securitySystemId") Long securitySystemId,
                                             @Param("employeeId") Long employeeId,
                                             @Param("roleName") String roleName);
}
