package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemProjection;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemWithActions;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.LocationRolesReplicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecuritySystemRepositoryIntegrationTest {

    @TestConfiguration
    static class Config {
        @Bean
        LocationRolesReplicaService locationRolesReplicaService(JdbcTemplate jdbcTemplate) {
            return new LocationRolesReplicaService(jdbcTemplate);
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @Autowired
    private SecuritySystemRepository repository;

    @Autowired
    private LocationRolesReplicaService locationRolesReplicaService;

    @Test
    void shouldSaveAndFindSecuritySystem() {
        // applicableActions.retainAll(actionsForRole)
        SecuritySystem securitySystem = new SecuritySystem("Oakland office",
            SecuritySystemState.ARMED);

        SecuritySystem saved = repository.save(securitySystem);
        assertThat(saved.getId()).isNotNull();

        Optional<SecuritySystem> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLocationName()).isEqualTo("Oakland office");
        assertThat(found.get().getState()).isEqualTo(SecuritySystemState.ARMED);
        // assertThat(found.get().getActions()).containsExactly(SecuritySystemAction.DISARM);
    }

    @Test
    void shouldFindAllSecuritySystems() {
        String customerEmployeeEmail = "customerEmployee%s@realguard.io".formatted(System.currentTimeMillis());

        long locationId = System.currentTimeMillis();

        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId, "SECURITY_SYSTEM_ARMER");
        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId, "SECURITY_SYSTEM_DISARMER");

        // Set.of(SecuritySystemAction.DISARM)

        SecuritySystem system1 = new SecuritySystem("Oakland office",
            SecuritySystemState.ARMED);
        system1.setLocationId(locationId);

        // Set.of(SecuritySystemAction.ARM)

        SecuritySystem system2 = new SecuritySystem("Berkeley office",
            SecuritySystemState.DISARMED);
        system2.setLocationId(locationId);

        repository.save(system1);
        repository.save(system2);

        var all = repository.findAllAccessible(customerEmployeeEmail)            .stream()
            .map(SecuritySystemProjection::toSecuritySystemWithActions)
            .toList();
        ;
        assertThat(all).hasSize(2);
        assertThat(all.stream().map(SecuritySystemWithActions::locationName).toList()).containsExactly("Oakland office", "Berkeley office");
        assertThat(all.stream().flatMap(securitySystemWithActions -> securitySystemWithActions.actions().stream()).toList())
            .containsExactlyInAnyOrder(SecuritySystemAction.ARM, SecuritySystemAction.DISARM);
    }

    @Test
    void shouldSaveAndRetrieveLocationId() {
        SecuritySystem securitySystem = new SecuritySystem("Oakland office",
            SecuritySystemState.CREATION_PENDING);
        securitySystem.setLocationId(123L);

        SecuritySystem saved = repository.save(securitySystem);
        assertThat(saved.getId()).isNotNull();

        Optional<SecuritySystem> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLocationId()).isEqualTo(123L);
    }

    @Test
    void shouldSaveAndRetrieveRejectionReason() {
        SecuritySystem securitySystem = new SecuritySystem("Oakland office",
            SecuritySystemState.CREATION_FAILED);
        securitySystem.setRejectionReason("Customer not found");

        SecuritySystem saved = repository.save(securitySystem);
        assertThat(saved.getId()).isNotNull();

        Optional<SecuritySystem> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRejectionReason()).isEqualTo("Customer not found");
        assertThat(found.get().getState()).isEqualTo(SecuritySystemState.CREATION_FAILED);
    }

    @Test
    void shouldHandleNullLocationIdAndRejectionReason() {
        SecuritySystem securitySystem = new SecuritySystem("Oakland office",
            SecuritySystemState.DISARMED);
        // Not setting locationId or rejectionReason - they should be null

        SecuritySystem saved = repository.save(securitySystem);
        
        Optional<SecuritySystem> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLocationId()).isNull();
        assertThat(found.get().getRejectionReason()).isNull();
    }
}