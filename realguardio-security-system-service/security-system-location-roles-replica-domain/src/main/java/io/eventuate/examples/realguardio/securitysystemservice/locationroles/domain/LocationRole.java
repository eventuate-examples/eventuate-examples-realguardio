package io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain;

public record LocationRole(
    Long id,
    String userName,
    Long locationId,
    String roleName
) {
}