package io.realguardio.osointegration.eventsubscribers;

import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class OsoEventSubscribersConfiguration {

    @Bean
    public DomainEventDispatcher domainEventDispatcher(
            DomainEventDispatcherFactory domainEventDispatcherFactory,
            CustomerEventConsumer eventConsumer) {
        return domainEventDispatcherFactory.make(
            "osoEventSubscribersDispatcher",
            eventConsumer.domainEventHandlers()
        );
    }
}
