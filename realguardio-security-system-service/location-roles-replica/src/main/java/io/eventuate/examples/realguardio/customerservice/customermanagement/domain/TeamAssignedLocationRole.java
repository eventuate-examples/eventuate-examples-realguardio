package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record TeamAssignedLocationRole(
    Long teamId,
    Long locationId,
    String roleName
) implements DomainEvent {
}
