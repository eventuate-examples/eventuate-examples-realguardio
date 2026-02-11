package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.domain.UserNameSupplier;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(classes = LocalAuthorizationIntegrationTest.Config.class)
@ActiveProfiles("UseOsoService")
@Testcontainers
public class LocalAuthorizationIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(LocalAuthorizationIntegrationTest.class);

    @Container
    public static OsoTestContainer osoDevServer = new OsoTestContainer()
            .withReuse(true)
            .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-service:"));

    @DynamicPropertySource
    static void setOsoProperties(DynamicPropertyRegistry registry) {
        osoDevServer.addProperties(registry);
    }

    @Configuration
    @Import(OsoSecuritySystemActionAuthorizerConfiguration.class)
    public static class Config {

    }

    @Autowired
    private RealGuardOsoFactManager realGuardOsoFactManager;

    @Autowired
    private RealGuardOsoAuthorizer  realGuardOsoAuthorizer;

    @MockitoBean
    private UserNameSupplier  userNameSupplier;

    @Test
    public void testLocalAuthorization() {
        realGuardOsoFactManager.createRoleInCustomer("alice", "acme", RolesAndPermissions.SECURITY_SYSTEM_ARMER);
        realGuardOsoFactManager.createLocationForCustomer("99", "acme");
        realGuardOsoFactManager.createLocationForCustomer("101", "acme");
        realGuardOsoFactManager.assignSecuritySystemToLocation("202", "99");
        realGuardOsoFactManager.assignSecuritySystemToLocation("203", "101");

        var sql = realGuardOsoAuthorizer.listLocal("alice", RolesAndPermissions.ARM, "SecuritySystem", "ss_id");
        assertThat(sql).isNotNull();
        System.out.println(sql);
    }

}
