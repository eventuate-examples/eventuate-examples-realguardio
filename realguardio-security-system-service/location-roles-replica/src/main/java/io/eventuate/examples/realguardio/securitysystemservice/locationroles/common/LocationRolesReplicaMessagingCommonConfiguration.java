package io.eventuate.examples.realguardio.securitysystemservice.locationroles.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LocationRolesReplicaMessagingCommonConfiguration {

    @Bean
    LocationRolesReplicaService  locationRolesReplicaService(JdbcTemplate jdbcTemplate) {
        return new LocationRolesReplicaService(jdbcTemplate);
    }
}