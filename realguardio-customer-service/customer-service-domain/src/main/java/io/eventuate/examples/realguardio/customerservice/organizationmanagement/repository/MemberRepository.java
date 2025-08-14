package io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Member;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Member entity.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    Optional<Member> findByEmailAddress(EmailAddress emailAddress);
}
