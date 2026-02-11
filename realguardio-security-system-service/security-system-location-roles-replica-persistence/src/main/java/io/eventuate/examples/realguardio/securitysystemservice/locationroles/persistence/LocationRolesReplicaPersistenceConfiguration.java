package io.eventuate.examples.realguardio.securitysystemservice.locationroles.persistence;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LocationRolesReplicaPersistenceConfiguration {

    @Bean
    LocationRolesRepository locationRolesRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcLocationRolesRepository(jdbcTemplate);
    }
}