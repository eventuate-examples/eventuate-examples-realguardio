package io.eventuate.examples.realguardio.securitysystemservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.securitysystemservice.db.DBInitializer;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystems;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForLocalTests;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuritySystemServiceIntegrationTest {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SecuritySystemServiceIntegrationTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

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
        String token = JwtTokenHelper.getJwtToken(iamService.getMappedPort(9000));
        
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
        assertThat(systems).extracting(SecuritySystem::getLocationName)
                .containsExactlyInAnyOrder(DBInitializer.LOCATION_OAKLAND_OFFICE, 
                        DBInitializer.LOCATION_BERKELEY_OFFICE, 
                        DBInitializer.LOCATION_HAYWARD_OFFICE);
        
        // Verify specific system details
        var oaklandOffice = systems.stream()
                .filter(s -> s.getLocationName().equals(DBInitializer.LOCATION_OAKLAND_OFFICE))
                .findFirst()
                .orElseThrow();
        assertThat(oaklandOffice.getState()).isEqualTo(SecuritySystemState.ARMED);
        assertThat(oaklandOffice.getActions()).containsExactly(SecuritySystemAction.DISARM);
        
        var haywardOffice = systems.stream()
                .filter(s -> s.getLocationName().equals(DBInitializer.LOCATION_HAYWARD_OFFICE))
                .findFirst()
                .orElseThrow();
        assertThat(haywardOffice.getState()).isEqualTo(SecuritySystemState.ALARMED);
        assertThat(haywardOffice.getActions()).containsExactlyInAnyOrder(
                SecuritySystemAction.ACKNOWLEDGE, SecuritySystemAction.DISARM);
    }
}