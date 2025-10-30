package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaMessagingCommonConfiguration;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaService;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
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

    @Bean
    public DomainEventDispatcher domainEventDispatcher(
            DomainEventDispatcherFactory domainEventDispatcherFactory,
            CustomerEmployeeLocationEventConsumer eventConsumer) {
        return domainEventDispatcherFactory.make(
                "locationRolesReplicaDispatcher",
                eventConsumer.domainEventHandlers()
        );
    }

}