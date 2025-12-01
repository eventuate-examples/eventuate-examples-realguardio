package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.common.testcontainers.EventuateVanillaPostgresContainer;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaService;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.tram.messaging.producer.MessageProducer;
import io.eventuate.tram.testing.producer.kafka.events.DirectToKafkaDomainEventPublisher;
import io.eventuate.tram.testing.producer.kafka.events.EnableDirectToKafkaDomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.lifecycle.Startables;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = LocationRolesReplicaIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("UseRolesReplica")
public class LocationRolesReplicaIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({LocationRolesReplicaMessagingConfiguration.class})
    @EnableDirectToKafkaDomainEventPublisher
    static class Config {
    }

    public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("location-replica-service-tests");

    public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
            .withNetworkAliases("kafka")
            .withReuse(false)
            ;

    static EventuateVanillaPostgresContainer postgres = new EventuateVanillaPostgresContainer();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        Startables.deepStart(eventuateKafkaCluster.kafka, postgres).join();

        eventuateKafkaCluster.kafka.registerProperties(registry::add);
        postgres.registerProperties(registry::add);
    }

    @Autowired
    private DirectToKafkaDomainEventPublisher eventPublisher;

    @Autowired
    private LocationRolesReplicaService locationRolesReplicaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MessageProducer messageProducer;

    @Test
    public void shouldConsumeEventAndUpdateDatabase() {
        // Given
        String userName = "john.doe@example.com";
        Long locationId = System.currentTimeMillis();
        String roleName = RolesAndPermissions.SECURITY_SYSTEM_ARMER;

        CustomerEmployeeAssignedLocationRole event =
                new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName);

        // When - publish the event
        eventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", "1", event);

        // Then - verify the database is updated using LocationRolesReplicaService
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
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
        String roleName = RolesAndPermissions.SECURITY_SYSTEM_VIEWER;

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
