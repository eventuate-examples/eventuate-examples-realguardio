package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;
import io.eventuate.tram.events.publisher.DomainEventPublisherForAggregate;

public interface CustomerEventPublisher
    extends DomainEventPublisherForAggregate<Customer, Long, CustomerEvent> {
}
