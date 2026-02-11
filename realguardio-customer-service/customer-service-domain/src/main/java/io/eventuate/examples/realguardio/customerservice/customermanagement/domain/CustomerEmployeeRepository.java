package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface CustomerEmployeeRepository extends JpaRepository<CustomerEmployee, Long>, CustomerEmployeeRepositoryCustom {

    Optional<CustomerEmployee> findByMemberIdAndCustomerId(Long memberId, Long customerId);

    /**
     * Find all role names for an employee within a specific customer.
     * Verifies that the employee is actually associated with the customer.
     *
     * @param customerId the ID of the customer
     * @param employeeUserId the user ID of the employee
     * @return set of role names the employee has in the customer
     */
    @Query("""
        SELECT mr.name
        FROM MemberRole mr, CustomerEmployee ce
        WHERE mr.member.id = ce.memberId
        AND mr.member.emailAddress.email = :employeeUserId
        AND ce.customerId = :customerId
        """)
    Set<String> findRolesInCustomer(@Param("customerId") Long customerId,
                                    @Param("employeeUserId") String employeeUserId);
}
