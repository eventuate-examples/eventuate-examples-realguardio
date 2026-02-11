package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesReplicaService;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.tram.messaging.producer.MessageProducer;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.tram.testing.producer.kafka.events.DirectToKafkaDomainEventPublisher;
import io.eventuate.tram.testing.producer.kafka.events.EnableDirectToKafkaDomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.eventuate.common.testcontainers.EventuateVanillaPostgresContainer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.lifecycle.Startables;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

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

    @MockitoBean
    private LocationRolesReplicaService locationRolesReplicaService;

    @MockitoBean
    private MessageProducer messageProducer;

    @Test
    public void shouldConsumeEventAndCallService() {
        // Given
        String userName = "john.doe@example.com";
        Long locationId = System.currentTimeMillis();
        String roleName = RolesAndPermissions.SECURITY_SYSTEM_ARMER;

        CustomerEmployeeAssignedLocationRole event =
                new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName);

        // When - publish the event
        eventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", "1", event);

        // Then - verify the service was called with the correct arguments
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(locationRolesReplicaService).saveLocationRole(userName, locationId, roleName);
        });
    }
}
