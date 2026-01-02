package io.realguardio.osointegration.eventsubscribers;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeAssignedCustomerRole;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationCreatedForCustomer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.SecuritySystemAssignedToLocation;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class CustomerEventConsumerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({OsoEventSubscribersConfiguration.class,
             TramInMemoryConfiguration.class})
    static class Config {
    }

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @MockBean
    private RealGuardOsoFactManager osoFactManager;

    @Test
    public void shouldHandleCustomerEmployeeAssignedCustomerRole() {
        Long customerEmployeeId = 123L;
        String userName = "admin@example.com";
        String customerId = "456";
        String roleName = "COMPANY_ROLE_ADMIN";

        CustomerEmployeeAssignedCustomerRole event =
            new CustomerEmployeeAssignedCustomerRole(customerEmployeeId, userName, roleName);

        domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId, Collections.singletonList(event));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(osoFactManager).createRoleInCustomer(
                userName,
                customerId,
                roleName
            );
        });
    }

    @Test
    public void shouldHandleLocationCreatedForCustomer() {
        Long locationId = 789L;
        String customerId = "456";

        LocationCreatedForCustomer event =
            new LocationCreatedForCustomer(locationId);

        domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId, Collections.singletonList(event));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(osoFactManager).createLocationForCustomer(
                locationId.toString(),
                customerId
            );
        });
    }

    @Test
    public void shouldHandleSecuritySystemAssignedToLocation() {
        Long locationId = 789L;
        Long securitySystemId = 101L;
        String customerId = "456";

        SecuritySystemAssignedToLocation event =
            new SecuritySystemAssignedToLocation(locationId, securitySystemId);

        domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId, Collections.singletonList(event));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(osoFactManager).assignSecuritySystemToLocation(
                securitySystemId.toString(),
                locationId.toString()
            );
        });
    }

    @Test
    public void shouldHandleCustomerEmployeeAssignedLocationRole() {
        String userName = "john.doe@example.com";
        Long locationId = 789L;
        String roleName = "SECURITY_SYSTEM_ARMER";
        String customerId = "456";

        CustomerEmployeeAssignedLocationRole event =
            new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName);

        domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId, Collections.singletonList(event));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(osoFactManager).createRoleAtLocation(
                userName,
                locationId.toString(),
                roleName
            );
        });
    }
}
