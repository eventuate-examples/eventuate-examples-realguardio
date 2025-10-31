package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;
import io.eventuate.tram.events.common.DomainEvent;

public record LocationCreatedForCustomer(Long locationId) implements DomainEvent, CustomerEvent {
}
