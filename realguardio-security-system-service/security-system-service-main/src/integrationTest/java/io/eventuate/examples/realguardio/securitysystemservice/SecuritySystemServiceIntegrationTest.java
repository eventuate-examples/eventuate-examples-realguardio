package io.eventuate.examples.realguardio.securitysystemservice;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystems;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.utility.DockerImageName;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecuritySystemServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    static GenericContainer<?> iamService;
    
    static {
        // Start containers in static block to ensure they're ready for DynamicPropertySource
        postgres.start();
        
        // IAM service is a mock authorization server - doesn't need PostgreSQL
        iamService = new GenericContainer<>(DockerImageName.parse("eventuate-examples-realguardio-realguardio-iam-service:latest"))
                .withExposedPorts(9000)
                .withEnv("SPRING_PROFILES_ACTIVE", "realguardio")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("IAM-SERVICE")))
                .waitingFor(Wait.forHttp("/actuator/health").forPort(9000).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(60));
        iamService.start();
    }
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", 
            () -> "http://localhost:" + iamService.getMappedPort(9000));
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            () -> "http://localhost:" + iamService.getMappedPort(9000) + "/oauth2/jwks");
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
                .containsExactlyInAnyOrder("Office Front Door", "Office Back Door", "Main Entrance");
        
        // Verify specific system details
        var frontDoor = systems.stream()
                .filter(s -> s.getLocationName().equals("Office Front Door"))
                .findFirst()
                .orElseThrow();
        assertThat(frontDoor.getState()).isEqualTo(SecuritySystemState.ARMED);
        assertThat(frontDoor.getActions()).containsExactly(SecuritySystemAction.DISARM);
        
        var mainEntrance = systems.stream()
                .filter(s -> s.getLocationName().equals("Main Entrance"))
                .findFirst()
                .orElseThrow();
        assertThat(mainEntrance.getState()).isEqualTo(SecuritySystemState.ALARMED);
        assertThat(mainEntrance.getActions()).containsExactlyInAnyOrder(
                SecuritySystemAction.ACKNOWLEDGE, SecuritySystemAction.DISARM);
    }
}