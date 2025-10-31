package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaMessagingCommonConfiguration;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(LocationRolesReplicaMessagingCommonConfiguration.class)
public class LocationRolesReplicaMessagingConfiguration {

    @Bean
    CustomerEmployeeLocationEventConsumer customerEmployeeLocationEventConsumer(LocationRolesReplicaService replicaService) {
        return new CustomerEmployeeLocationEventConsumer(replicaService);
    }

}