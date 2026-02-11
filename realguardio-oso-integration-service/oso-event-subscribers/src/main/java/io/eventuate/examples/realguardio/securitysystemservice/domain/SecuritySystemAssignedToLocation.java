package io.eventuate.examples.realguardio.securitysystemservice.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record SecuritySystemAssignedToLocation(Long securitySystemId, Long locationId) implements DomainEvent {
}
