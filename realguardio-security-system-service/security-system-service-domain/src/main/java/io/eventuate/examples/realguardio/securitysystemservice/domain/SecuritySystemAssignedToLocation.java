package io.eventuate.examples.realguardio.securitysystemservice.domain;

public record SecuritySystemAssignedToLocation(Long securitySystemId, Long locationId) implements SecuritySystemEvent {
}
