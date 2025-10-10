package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record CustomerEmployeeAssignedCustomerRole(Long customerEmployeeId, String roleName) implements DomainEvent {
}
