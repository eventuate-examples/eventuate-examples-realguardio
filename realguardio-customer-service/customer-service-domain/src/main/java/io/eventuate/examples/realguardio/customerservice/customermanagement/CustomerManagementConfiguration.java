package io.eventuate.examples.realguardio.customerservice.customermanagement;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.*;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.OrganizationManagementConfiguration;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.MemberService;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.OrganizationService;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories
@EntityScan
@Import(OrganizationManagementConfiguration.class)
public class CustomerManagementConfiguration {

  @Bean
  public CustomerService customerService(CustomerRepository customerRepository,
                                         CustomerEmployeeRepository customerEmployeeRepository,
                                         LocationRepository locationRepository,
                                         CustomerEmployeeLocationRoleRepository customerEmployeeLocationRoleRepository,
                                         TeamRepository teamRepository,
                                         TeamLocationRoleRepository teamLocationRoleRepository,
                                         OrganizationService organizationService,
                                         MemberService memberService,
                                         UserNameSupplier userNameSupplier,
                                         CustomerEventPublisher customerEventPublisher) {
    return new CustomerService(
        customerRepository,
        customerEmployeeRepository,
        locationRepository,
        customerEmployeeLocationRoleRepository,
        teamRepository,
        teamLocationRoleRepository,
        organizationService,
        memberService,
        userNameSupplier,
        customerEventPublisher);
  }
}
