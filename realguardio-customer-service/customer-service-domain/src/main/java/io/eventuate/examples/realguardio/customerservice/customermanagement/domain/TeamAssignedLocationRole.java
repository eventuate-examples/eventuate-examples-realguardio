package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamAssignedLocationRole(
    Long teamId,
    Long locationId,
    String roleName
) implements CustomerEvent {
}
