package io.eventuate.examples.realguardio.customerservice.customermanagement.eventpublishing;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeAssignedCustomerRole;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEventPublisher;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationCreatedForCustomer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.SecuritySystemAssignedToLocation;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamAssignedLocationRole;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.TeamMemberAdded;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.tram.spring.testing.cloudcontract.EnableEventuateTramContractVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {BaseClass.TestConfig.class})
public abstract class BaseClass {

    @Autowired
    private CustomerEventPublisher customerEventPublisher;

    private Customer createCustomerWithId(Long id) {
        Customer customer = new Customer("Test Customer", 1L);
        try {
            var idField = Customer.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(customer, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set customer ID", e);
        }
        return customer;
    }

    public void locationCreatedForCustomer() {
        customerEventPublisher.publish(createCustomerWithId(123L), new LocationCreatedForCustomer(101L));
    }

    public void teamAssignedLocationRole() {
        customerEventPublisher.publish(createCustomerWithId(123L), new TeamAssignedLocationRole(201L, 101L, "Admin"));
    }

    public void teamMemberAdded() {
        customerEventPublisher.publish(createCustomerWithId(123L), new TeamMemberAdded(201L, 301L));
    }

    public void customerEmployeeAssignedLocationRole() {
        customerEventPublisher.publish(createCustomerWithId(123L), new CustomerEmployeeAssignedLocationRole("john.doe", 101L, "Manager"));
    }

    public void securitySystemAssignedToLocation() {
        customerEventPublisher.publish(createCustomerWithId(123L), new SecuritySystemAssignedToLocation(101L, 401L));
    }

    public void customerEmployeeAssignedCustomerRole() {
        customerEventPublisher.publish(createCustomerWithId(123L), new CustomerEmployeeAssignedCustomerRole(301L, "Owner"));
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableEventuateTramContractVerifier
    @Import(CustomerManagementEventPublishingConfiguration.class)
    public static class TestConfig {

    }


}
