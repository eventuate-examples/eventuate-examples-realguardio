package io.eventuate.examples.realguardio.securitysystemservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.securitysystemservice.db.DBInitializer;
import io.eventuate.examples.realguardio.securitysystemservice.domain.*;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaService;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForLocalTests;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuritySystemServiceIntegrationTest {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SecuritySystemServiceIntegrationTest.class);

    @TestConfiguration
    public static class Config {
        @Bean
        UserService userService() {
            return new UserServiceImpl();
        }

    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private LocationRolesReplicaService locationRolesReplicaService;

    @Autowired
    private DBInitializer dbInitializer;

    public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("customer-service-tests");

    public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
        .withNetworkAliases("kafka")
        .withReuse(true)
        ;

    public static EventuateDatabaseContainer<?> database = DatabaseContainerFactory.makeVanillaDatabaseContainer()
        .withNetwork(eventuateKafkaCluster.network)
        .withNetworkAliases("database")
        .withReuse(true)
        ;

    static AuthorizationServerContainerForLocalTests iamService = new AuthorizationServerContainerForLocalTests(Path.of("../../realguardio-iam-service/Dockerfile"))
        .withUserDb()
        .withReuse(true)
        .withNetworkAliases("database")
        .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("IAM"));


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Startables.deepStart(database, kafka, iamService).join();

        kafka.registerProperties(registry::add);
        database.registerProperties(registry::add);

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
            () -> "http://localhost:" + iamService.getFirstMappedPort());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            () -> "http://localhost:" + iamService.getFirstMappedPort() + "/oauth2/jwks");
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/securitysystems",
                HttpMethod.GET,
                null,
                String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnSecuritySystemsWithValidToken() {
        // Get JWT token from IAM service
        String customerEmployeeEmail = "customerEmployee%s@realguard.io".formatted(System.currentTimeMillis());
        userService.createCustomerEmployeeUser(customerEmployeeEmail);

        long baseLocationId = System.currentTimeMillis();

        dbInitializer.initializeForLocation(baseLocationId);

        // Set up location roles for all three locations
        for (int i = 0; i < 3; i++) {
            long locationId = baseLocationId + i;
            locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId, RolesAndPermissions.SECURITY_SYSTEM_ARMER);
            locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId, RolesAndPermissions.SECURITY_SYSTEM_DISARMER);
            locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId, "SECURITY_SYSTEM_ACKNOWLEDGER");
        }

        String token = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort(), null, customerEmployeeEmail, "password");
        
        // Make request with token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<SecuritySystems> response = restTemplate.exchange(
                "/securitysystems",
                HttpMethod.GET,
                entity,
                SecuritySystems.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().securitySystems()).isNotNull();
        assertThat(response.getBody().securitySystems()).hasSize(3);
        
        // Verify the data
        var systems = response.getBody().securitySystems();
        assertThat(systems).extracting(SecuritySystemWithActions::locationName)
                .containsExactlyInAnyOrder(DBInitializer.LOCATION_OAKLAND_OFFICE,
                        DBInitializer.LOCATION_BERKELEY_OFFICE,
                        DBInitializer.LOCATION_HAYWARD_OFFICE);
        
        // Verify specific system details
        var oaklandOffice = systems.stream()
                .filter(s -> s.locationName().equals(DBInitializer.LOCATION_OAKLAND_OFFICE))
                .findFirst()
                .orElseThrow();
        assertThat(oaklandOffice.state()).isEqualTo(SecuritySystemState.ARMED);
        assertThat(oaklandOffice.actions()).containsExactly(SecuritySystemAction.DISARM);
        
        var haywardOffice = systems.stream()
                .filter(s -> s.locationName().equals(DBInitializer.LOCATION_HAYWARD_OFFICE))
                .findFirst()
                .orElseThrow();
        assertThat(haywardOffice.state()).isEqualTo(SecuritySystemState.ALARMED);
        assertThat(haywardOffice.actions()).containsExactlyInAnyOrder(SecuritySystemAction.ACKNOWLEDGE, SecuritySystemAction.DISARM);
    }
}