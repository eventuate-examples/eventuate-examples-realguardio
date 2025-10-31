package io.eventuate.examples.realguardio.securitysystemservice.locationroles.common;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging.LocationRolesReplicaMessagingConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class CustomerEmployeeLocationEventConsumerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({LocationRolesReplicaMessagingConfiguration.class,
             TramInMemoryConfiguration.class})
    static class Config {
    }

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private LocationRolesReplicaService locationRolesReplicaService;


    @Test
    public void shouldConsumeEventAndUpdateDatabase() {
        // Given
        String userName = "john.doe@example.com";
        Long locationId = 123L;
        String roleName = RolesAndPermissions.SECURITY_SYSTEM_ARMER;
        
        CustomerEmployeeAssignedLocationRole event = 
            new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName);

        // When - publish the event
        domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", "1", Collections.singletonList(event));

        // Then - verify the database is updated using LocationRolesReplicaService
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(locationRolesReplicaService).saveLocationRole(userName, locationId, roleName);
        });
    }

}