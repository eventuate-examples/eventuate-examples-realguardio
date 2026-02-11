package io.eventuate.examples.realguardio.securitysystemservice.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepositoryWithOso;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesReplicaService;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.persistence.JdbcLocationRolesRepository;
import io.eventuate.examples.realguardio.securitysystemservice.osointegration.OsoSecuritySystemActionAuthorizerConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles({"UseOsoService", "OsoLocalSecuritySystemLocation"})
public class SecuritySystemRepositoryWithOsoLocalIntegrationTest extends AbstractSecuritySystemRepositoryIntegrationTest {

    @TestConfiguration
    @Import(OsoSecuritySystemActionAuthorizerConfiguration.class)
    static class Config {
        @Bean
        LocationRolesReplicaService locationRolesReplicaService(JdbcTemplate jdbcTemplate) {
            return new LocationRolesReplicaService(new JdbcLocationRolesRepository(jdbcTemplate));
        }
    }

    @Autowired
    protected SecuritySystemRepositoryWithOso securitySystemRepositoryWithOso;

    @Autowired
    private RealGuardOsoFactManager realGuardOsoFactManager;

    @Test
    void shouldFindWithOso() {

        String customerEmployeeEmail = "customerEmployee%s@realguard.io".formatted(System.currentTimeMillis());
//        String customerEmployeeEmail = "'X' or 'Y'";
        String company = "acme" + System.currentTimeMillis();

        long locationId = System.currentTimeMillis();
        String locationIdAsString = Long.toString(locationId);



        SecuritySystem system1 = new SecuritySystem("Oakland office",
                SecuritySystemState.ARMED);
        system1.setLocationId(locationId);

        repository.save(system1);

        realGuardOsoFactManager.createRoleInCustomer(customerEmployeeEmail, company, RolesAndPermissions.SECURITY_SYSTEM_ARMER);
        realGuardOsoFactManager.createLocationForCustomer(locationIdAsString, company);
        realGuardOsoFactManager.createLocationForCustomer(locationIdAsString + "2", company);
        // realGuardOsoFactManager.assignSecuritySystemToLocation(Long.toString(system1.getId()), locationIdAsString);

        System.out.println("customerEmployeeEmail: " + customerEmployeeEmail);
        System.out.println("locationId: " + locationId);

        var results = securitySystemRepositoryWithOso.findAllAccessible(customerEmployeeEmail);
        System.out.println(results);

        assertThat(results).hasSize(1);
        var result = results.get(0);
        assertThat(result.getId()).isEqualTo(system1.getId());

//        assertThat(Arrays.asList(results.get(0).getRoleNames())).containsExactly(RolesAndPermissions.SECURITY_SYSTEM_ARMER);
    }

}
