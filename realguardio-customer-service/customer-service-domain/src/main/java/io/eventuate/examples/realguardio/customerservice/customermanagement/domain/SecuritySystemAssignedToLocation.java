package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record SecuritySystemAssignedToLocation(Long locationId, Long   securitySystemId) implements CustomerEvent {
}
