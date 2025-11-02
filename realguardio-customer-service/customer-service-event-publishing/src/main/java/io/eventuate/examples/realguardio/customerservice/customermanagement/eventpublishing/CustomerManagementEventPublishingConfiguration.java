package io.eventuate.examples.realguardio.customerservice.customermanagement.eventpublishing;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEventPublisher;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerManagementEventPublishingConfiguration {

    @Bean
    public CustomerEventPublisher customerEventPublisher(DomainEventPublisher domainEventPublisher) {
        return new CustomerEventPublisherImpl(domainEventPublisher);
    }

}
