package io.eventuate.examples.realguardio.securitysystemservice.eventpublishing;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemEvent;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemEventPublisher;
import io.eventuate.tram.events.publisher.AbstractDomainEventPublisherForAggregateImpl;
import io.eventuate.tram.events.publisher.DomainEventPublisher;

public class SecuritySystemEventPublisherImpl
    extends AbstractDomainEventPublisherForAggregateImpl<SecuritySystem, Long, SecuritySystemEvent>
    implements SecuritySystemEventPublisher {

    public SecuritySystemEventPublisherImpl(DomainEventPublisher domainEventPublisher) {
        super(SecuritySystem.class, SecuritySystem::getId, domainEventPublisher, SecuritySystemEvent.class);
    }
}
