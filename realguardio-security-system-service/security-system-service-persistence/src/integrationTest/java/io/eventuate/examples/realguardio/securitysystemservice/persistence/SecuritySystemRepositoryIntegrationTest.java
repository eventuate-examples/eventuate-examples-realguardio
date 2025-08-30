package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SecuritySystemRepositoryIntegrationTest {

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

    @Test
    void shouldSaveAndFindSecuritySystem() {
        SecuritySystem securitySystem = new SecuritySystem("Oakland office", 
            SecuritySystemState.ARMED, 
            Set.of(SecuritySystemAction.DISARM));

        SecuritySystem saved = repository.save(securitySystem);
        assertThat(saved.getId()).isNotNull();

        Optional<SecuritySystem> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLocationName()).isEqualTo("Oakland office");
        assertThat(found.get().getState()).isEqualTo(SecuritySystemState.ARMED);
        assertThat(found.get().getActions()).containsExactly(SecuritySystemAction.DISARM);
    }

    @Test
    void shouldFindAllSecuritySystems() {
        SecuritySystem system1 = new SecuritySystem("Oakland office",
            SecuritySystemState.ARMED,
            Set.of(SecuritySystemAction.DISARM));

        SecuritySystem system2 = new SecuritySystem("Berkeley office",
            SecuritySystemState.DISARMED,
            Set.of(SecuritySystemAction.ARM));

        repository.save(system1);
        repository.save(system2);

        Iterable<SecuritySystem> all = repository.findAll();
        assertThat(all).hasSize(2);
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