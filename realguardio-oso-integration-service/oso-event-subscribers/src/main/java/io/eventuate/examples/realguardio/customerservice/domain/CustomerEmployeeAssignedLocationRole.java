package io.eventuate.examples.realguardio.customerservice.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record CustomerEmployeeAssignedLocationRole(
    String userName,
    Long locationId,
    String roleName
) implements DomainEvent {
}
