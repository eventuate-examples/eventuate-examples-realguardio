package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record SecuritySystemAssignedToLocation(Long locationId, Long   securitySystemId) implements DomainEvent {
}
