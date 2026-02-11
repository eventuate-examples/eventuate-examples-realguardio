package io.eventuate.examples.realguardio.securitysystemservice.domain;

import io.eventuate.tram.events.publisher.DomainEventPublisherForAggregate;

public interface SecuritySystemEventPublisher
    extends DomainEventPublisherForAggregate<SecuritySystem, Long, SecuritySystemEvent> {
}
