package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.domain.*;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesReplicaService;
import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class AbstractSecuritySystemRepositoryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSecuritySystemRepositoryIntegrationTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    public static OsoTestContainer osoDevServer = new OsoTestContainer()
            .withReuse(true)
            .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-service:"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        osoDevServer.addProperties(registry);
    }


    @Autowired
    protected SecuritySystemRepository repository;

    @Autowired
    protected LocationRolesReplicaService locationRolesReplicaService;


    @MockitoBean
    private UserNameSupplier  userNameSupplier;

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

        long locationId1 = System.currentTimeMillis();
        long locationId2 = locationId1 + 1;

        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId1, RolesAndPermissions.SECURITY_SYSTEM_ARMER);
        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId1, RolesAndPermissions.SECURITY_SYSTEM_DISARMER);
        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId2, RolesAndPermissions.SECURITY_SYSTEM_ARMER);
        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId2, RolesAndPermissions.SECURITY_SYSTEM_DISARMER);

        // Set.of(SecuritySystemAction.DISARM)

        SecuritySystem system1 = new SecuritySystem("Oakland office",
            SecuritySystemState.ARMED);
        system1.setLocationId(locationId1);

        // Set.of(SecuritySystemAction.ARM)

        SecuritySystem system2 = new SecuritySystem("Berkeley office",
            SecuritySystemState.DISARMED);
        system2.setLocationId(locationId2);

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
            SecuritySystemState.DISARMED);
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

    @Test
    void shouldEnforceUniqueLocationIdConstraint() {
        Long locationId = System.currentTimeMillis();

        SecuritySystem first = new SecuritySystem("First Office", SecuritySystemState.DISARMED);
        first.setLocationId(locationId);
        repository.save(first);

        SecuritySystem second = new SecuritySystem("Second Office", SecuritySystemState.DISARMED);
        second.setLocationId(locationId);

        assertThatThrownBy(() -> repository.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

}