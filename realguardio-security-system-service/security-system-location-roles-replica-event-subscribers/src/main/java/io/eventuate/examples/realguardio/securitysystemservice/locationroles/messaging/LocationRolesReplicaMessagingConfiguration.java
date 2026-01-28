package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesReplicaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocationRolesReplicaMessagingConfiguration {

    @Bean
    CustomerEmployeeLocationEventConsumer customerEmployeeLocationEventConsumer(LocationRolesReplicaService replicaService) {
        return new CustomerEmployeeLocationEventConsumer(replicaService);
    }
}