package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.*;
import io.eventuate.examples.realguardio.securitysystemservice.persistence.JPAPersistenceConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"UseOsoService", "OsoLocalSecuritySystemLocation"})
@Import({OsoSecuritySystemActionAuthorizerConfiguration.class, JPAPersistenceConfiguration.class})
public class OsoLocalSecuritySystemActionAuthorizerIntegrationTest {

    @SpringBootApplication
    static class TestConfig {
    }

    private static final Logger logger = LoggerFactory.getLogger(OsoLocalSecuritySystemActionAuthorizerIntegrationTest.class);

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
    private SecuritySystemRepository securitySystemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RealGuardOsoAuthorizer realGuardOsoAuthorizer;

    @Autowired
    private RealGuardOsoFactManager realGuardOsoFactManager;

    @Autowired
    private SecuritySystemActionAuthorizer securitySystemActionAuthorizer;

    @MockitoBean
    private UserNameSupplier userNameSupplier;

    private String customerEmployeeEmail;
    private String company;
    private long locationId;
    private SecuritySystem securitySystem;

    @BeforeEach
    void setUp() {
        customerEmployeeEmail = "employee%s@realguard.io".formatted(System.currentTimeMillis());
        company = "acme" + System.currentTimeMillis();
        locationId = System.currentTimeMillis();

        // Create security system with locationId in local database
        securitySystem = new SecuritySystem("Oakland office", SecuritySystemState.ARMED);
        securitySystem.setLocationId(locationId);
        securitySystem = securitySystemRepository.save(securitySystem);

        // Create Location-Customer relation in Oso Cloud
        realGuardOsoFactManager.createLocationForCustomer(String.valueOf(locationId), company);

        // NOTE: We do NOT create SecuritySystem-Location relation in Oso Cloud
        // That relationship is derived from local data bindings (the security_system.location_id column)
    }

    @Test
    void shouldAuthorizeWhenUserHasRoleAtLocation() {
        // Create role at location in Oso Cloud
        realGuardOsoFactManager.createRoleAtLocation(customerEmployeeEmail, String.valueOf(locationId), RolesAndPermissions.SECURITY_SYSTEM_DISARMER);

        when(userNameSupplier.getCurrentUserName()).thenReturn(customerEmployeeEmail);

        // This should succeed - user has DISARMER role at the location, and the
        // SecuritySystem-Location relationship is resolved via local data bindings
        securitySystemActionAuthorizer.verifyCanDo(securitySystem.getId(), "disarm");
    }

    @Test
    void shouldDenyWhenUserLacksRoleAtLocation() {
        // User has no role at this location
        when(userNameSupplier.getCurrentUserName()).thenReturn(customerEmployeeEmail);

        assertThatThrownBy(() -> securitySystemActionAuthorizer.verifyCanDo(securitySystem.getId(), "disarm"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldDenyWhenSecuritySystemNotAtUsersLocation() {
        // Create a different location and give user role there
        long differentLocationId = locationId + 1000;
        realGuardOsoFactManager.createLocationForCustomer(String.valueOf(differentLocationId), company);
        realGuardOsoFactManager.createRoleAtLocation(customerEmployeeEmail, String.valueOf(differentLocationId), RolesAndPermissions.SECURITY_SYSTEM_DISARMER);

        when(userNameSupplier.getCurrentUserName()).thenReturn(customerEmployeeEmail);

        // User has role at a different location, not the one the security system is at
        assertThatThrownBy(() -> securitySystemActionAuthorizer.verifyCanDo(securitySystem.getId(), "disarm"))
                .isInstanceOf(ForbiddenException.class);
    }
}
