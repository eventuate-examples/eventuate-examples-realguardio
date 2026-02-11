package io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class LocationRolesReplicaDomainConfiguration {

    @Bean
    LocationRolesReplicaService locationRolesReplicaService(LocationRolesRepository locationRolesRepository) {
        return new LocationRolesReplicaService(locationRolesRepository);
    }

    @Bean
    @Profile("UseRolesReplica")
    CustomerServiceClientReplicaImpl customerServiceClientReplicaImpl(LocationRolesReplicaService locationRolesReplicaService) {
        return new CustomerServiceClientReplicaImpl(locationRolesReplicaService);
    }
}
