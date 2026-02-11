package io.eventuate.examples.realguardio.customerservice.domain;

import io.eventuate.tram.events.common.DomainEvent;

public interface CustomerEvent extends DomainEvent {
    // Marker interface for all customer events
}
