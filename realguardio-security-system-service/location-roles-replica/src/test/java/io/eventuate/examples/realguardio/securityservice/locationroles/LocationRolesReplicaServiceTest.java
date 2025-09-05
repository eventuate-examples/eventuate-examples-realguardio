package io.eventuate.examples.realguardio.securityservice.locationroles;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Sql(scripts = "/schema.sql")
public class LocationRolesReplicaServiceTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({LocationRolesReplicaConfiguration.class,
             TramInMemoryConfiguration.class})
    static class Config {
    }

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LocationRolesReplicaService locationRolesReplicaService;

    @Test
    public void shouldConsumeEventAndUpdateDatabase() {
        // Given
        String userName = "john.doe@example.com";
        Long locationId = 123L;
        String roleName = "SECURITY_SYSTEM_ARMER";
        
        CustomerEmployeeAssignedLocationRole event = 
            new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName);

        // When - publish the event
        domainEventPublisher.publish("Customer", "1", Collections.singletonList(event));

        // Then - verify the database is updated using LocationRolesReplicaService
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<LocationRole> results = locationRolesReplicaService.findLocationRoles(userName, locationId);
            
            assertThat(results).hasSize(1);
            LocationRole role = results.get(0);
            assertThat(role.userName()).isEqualTo(userName);
            assertThat(role.locationId()).isEqualTo(locationId);
            assertThat(role.roleName()).isEqualTo(roleName);
        });
    }

    @Test
    public void shouldFindLocationRoles() {
        // Given - insert test data directly
        String userName = "jane.smith@example.com";
        Long locationId = 456L;
        String roleName = "SECURITY_SYSTEM_VIEWER";
        
        jdbcTemplate.update(
            "INSERT INTO customer_employee_location_role (user_name, location_id, role_name) VALUES (?, ?, ?)",
            userName, locationId, roleName
        );

        // When - call findLocationRoles
        List<LocationRole> results = locationRolesReplicaService.findLocationRoles(userName, locationId);

        // Then - verify the correct data is returned
        assertThat(results).hasSize(1);
        LocationRole role = results.get(0);
        assertThat(role.userName()).isEqualTo(userName);
        assertThat(role.locationId()).isEqualTo(locationId);
        assertThat(role.roleName()).isEqualTo(roleName);
    }

}