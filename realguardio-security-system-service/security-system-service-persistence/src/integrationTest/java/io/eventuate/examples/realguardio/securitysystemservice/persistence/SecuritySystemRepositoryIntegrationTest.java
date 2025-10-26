package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.LocationRolesReplicaService;
import io.eventuate.examples.realguardio.securitysystemservice.osointegration.OsoSecuritySystemActionAuthorizerConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SecuritySystemRepositoryIntegrationTest extends AbstractSecuritySystemRepositoryIntegrationTest {

    @TestConfiguration
    @Import(OsoSecuritySystemActionAuthorizerConfiguration.class)
    static class Config {
        @Bean
        LocationRolesReplicaService locationRolesReplicaService(JdbcTemplate jdbcTemplate) {
            return new LocationRolesReplicaService(jdbcTemplate);
        }
    }

}
