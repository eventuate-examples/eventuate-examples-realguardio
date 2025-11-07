package io.eventuate.examples.realguardio.customerservice.customermanagement.persistence;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = CustomerRepository.class)
@EntityScan(basePackageClasses = Customer.class)
public class CustomerManagementJpaPersistenceConfiguration {
}
