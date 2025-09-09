package io.eventuate.examples.realguardio.securitysystemservice.locationroles;

import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EntityScan
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