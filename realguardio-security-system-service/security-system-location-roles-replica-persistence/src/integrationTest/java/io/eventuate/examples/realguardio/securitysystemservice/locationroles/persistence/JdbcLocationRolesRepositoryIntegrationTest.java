package io.eventuate.examples.realguardio.securitysystemservice.locationroles.persistence;

import io.eventuate.common.testcontainers.EventuateVanillaPostgresContainer;
import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JdbcLocationRolesRepositoryIntegrationTest.Config.class)
public class JdbcLocationRolesRepositoryIntegrationTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(LocationRolesReplicaPersistenceConfiguration.class)
    static class Config {
    }

    static EventuateVanillaPostgresContainer postgres = new EventuateVanillaPostgresContainer();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        postgres.registerProperties(registry::add);
    }

    @Autowired
    private LocationRolesRepository locationRolesRepository;

    @Test
    public void shouldSaveAndFindLocationRoles() {
        String userName = "jane.smith@example.com";
        Long locationId = System.currentTimeMillis();
        String roleName = RolesAndPermissions.SECURITY_SYSTEM_VIEWER;

        locationRolesRepository.saveLocationRole(userName, locationId, roleName);

        List<LocationRole> results = locationRolesRepository.findLocationRoles(userName, locationId);

        assertThat(results).hasSize(1);
        LocationRole role = results.get(0);
        assertThat(role.userName()).isEqualTo(userName);
        assertThat(role.locationId()).isEqualTo(locationId);
        assertThat(role.roleName()).isEqualTo(roleName);
    }
}
