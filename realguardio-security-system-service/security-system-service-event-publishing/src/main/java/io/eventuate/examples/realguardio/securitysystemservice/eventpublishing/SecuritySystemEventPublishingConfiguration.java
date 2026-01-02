package io.eventuate.examples.realguardio.securitysystemservice.eventpublishing;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemEventPublisher;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecuritySystemEventPublishingConfiguration {

    @Bean
    public SecuritySystemEventPublisher securitySystemEventPublisher(DomainEventPublisher domainEventPublisher) {
        return new SecuritySystemEventPublisherImpl(domainEventPublisher);
    }
}
