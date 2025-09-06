package io.eventuate.examples.realguardio.securitysystemservice.locationroles;

import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class LocationRolesReplicaConfiguration {
    
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