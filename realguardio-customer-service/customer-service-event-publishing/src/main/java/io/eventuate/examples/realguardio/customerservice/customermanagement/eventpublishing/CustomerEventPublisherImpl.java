package io.eventuate.examples.realguardio.customerservice.customermanagement.eventpublishing;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEventPublisher;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;
import io.eventuate.tram.events.publisher.AbstractDomainEventPublisherForAggregateImpl;
import io.eventuate.tram.events.publisher.DomainEventPublisher;

public class CustomerEventPublisherImpl
    extends AbstractDomainEventPublisherForAggregateImpl<Customer, Long, CustomerEvent>
    implements CustomerEventPublisher {

    public CustomerEventPublisherImpl(DomainEventPublisher domainEventPublisher) {
        super(Customer.class, Customer::getId, domainEventPublisher, CustomerEvent.class);
    }
}
