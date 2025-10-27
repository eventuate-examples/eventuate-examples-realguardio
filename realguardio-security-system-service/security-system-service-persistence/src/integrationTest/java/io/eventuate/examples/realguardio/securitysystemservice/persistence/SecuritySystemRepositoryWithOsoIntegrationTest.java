package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepositoryWithOso;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.LocationRolesReplicaService;
import io.eventuate.examples.realguardio.securitysystemservice.osointegration.OsoSecuritySystemActionAuthorizerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("UseOsoService")
public class SecuritySystemRepositoryWithOsoIntegrationTest extends AbstractSecuritySystemRepositoryIntegrationTest {

    @TestConfiguration
    @Import(OsoSecuritySystemActionAuthorizerConfiguration.class)
    static class Config {
        @Bean
        LocationRolesReplicaService locationRolesReplicaService(JdbcTemplate jdbcTemplate) {
            return new LocationRolesReplicaService(jdbcTemplate);
        }
    }

    @Autowired
    protected SecuritySystemRepositoryWithOso securitySystemRepositoryWithOso;


    @Test
    void shouldFindWithOso() {

//        String customerEmployeeEmail = "customerEmployee%s@realguard.io".formatted(System.currentTimeMillis());
        String customerEmployeeEmail = "'X' or 'Y'";

        long locationId = System.currentTimeMillis();

        locationRolesReplicaService.saveLocationRole(customerEmployeeEmail, locationId, "SECURITY_SYSTEM_ARMER");

        SecuritySystem system1 = new SecuritySystem("Oakland office",
                SecuritySystemState.ARMED);
        system1.setLocationId(locationId);

        repository.save(system1);

        System.out.println("customerEmployeeEmail: " + customerEmployeeEmail);
        System.out.println("locationId: " + locationId);

        var results = securitySystemRepositoryWithOso.findAllAccessible(customerEmployeeEmail);
        assertThat(results).isNotEmpty();

        System.out.println(results);
        assertThat(Arrays.asList(results.get(0).getRoleNames())).containsExactly("SECURITY_SYSTEM_ARMER");
    }

}
