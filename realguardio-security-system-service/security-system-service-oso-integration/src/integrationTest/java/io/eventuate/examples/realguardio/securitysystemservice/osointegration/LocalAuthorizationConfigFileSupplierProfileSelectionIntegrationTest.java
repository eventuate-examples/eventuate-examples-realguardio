package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.UserNameSupplier;
import io.realguardio.osointegration.ososervice.LocalAuthorizationConfigFileSupplier;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LocalAuthorizationConfigFileSupplierProfileSelectionIntegrationTest.Config.class)
@ActiveProfiles({"UseOsoService", "OsoLocalSecuritySystemLocation"})
@Testcontainers
public class LocalAuthorizationConfigFileSupplierProfileSelectionIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(LocalAuthorizationConfigFileSupplierProfileSelectionIntegrationTest.class);

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
    private LocalAuthorizationConfigFileSupplier localAuthorizationConfigFileSupplier;

    @MockitoBean
    private UserNameSupplier userNameSupplier;

    @Test
    void shouldLoadConfigFileWithSecuritySystemLocation() {
        var path = localAuthorizationConfigFileSupplier.get();
        assertThat(path.toString()).contains("local_authorization_config_with_security_system_location.yaml");
    }
}
