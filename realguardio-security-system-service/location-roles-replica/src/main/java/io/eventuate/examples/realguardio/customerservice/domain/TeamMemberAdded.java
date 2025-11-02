package io.eventuate.examples.realguardio.customerservice.domain;

import io.eventuate.tram.events.common.DomainEvent;

public record TeamMemberAdded(
    Long teamId,
    Long customerEmployeeId
) implements DomainEvent {
}
