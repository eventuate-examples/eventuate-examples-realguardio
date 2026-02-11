package io.eventuate.examples.realguardio.customerservice.domain;

public record CustomerEmployeeAssignedLocationRole(
    String userName,
    Long locationId,
    String roleName
) implements CustomerEvent {
}