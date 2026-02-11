package io.eventuate.examples.realguardio.customerservice.organizationmanagement.persistence;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.domain.Organization;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.repository.OrganizationRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = OrganizationRepository.class)
@EntityScan(basePackageClasses = Organization.class)
public class OrganizationManagementJpaPersistenceConfiguration {
}
