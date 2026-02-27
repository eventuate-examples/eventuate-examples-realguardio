package io.eventuate.examples.realguardio.customerservice.osointegration;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
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
    @Import(OsoCustomerActionAuthorizerConfiguration.class)
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
        realGuardOsoFactManager.createRoleInCustomer("alice@example.com", "101", RolesAndPermissions.Roles.COMPANY_ROLE_ADMIN);
        realGuardOsoFactManager.createRoleInCustomer("alice@example.com", "102", RolesAndPermissions.Roles.COMPANY_ROLE_ADMIN);

        var sql = realGuardOsoAuthorizer.listLocal("alice@example.com", RolesAndPermissions.Permissions.CREATE_CUSTOMER_EMPLOYEE, "Customer", "customer_id");
        assertThat(sql).isNotNull();
        System.out.println(sql);
    }

}
