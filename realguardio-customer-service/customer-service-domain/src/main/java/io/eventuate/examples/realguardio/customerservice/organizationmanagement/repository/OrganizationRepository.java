package io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Organization entity.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /**
     * Find organizations by name.
     * 
     * @param name the name to search for
     * @return a list of organizations with the given name
     */
    java.util.List<Organization> findByName(String name);

}
