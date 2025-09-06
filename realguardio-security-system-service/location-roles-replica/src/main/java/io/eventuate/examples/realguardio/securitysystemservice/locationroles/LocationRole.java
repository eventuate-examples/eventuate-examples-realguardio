package io.eventuate.examples.realguardio.securitysystemservice.locationroles;

public record LocationRole(
    Long id,
    String userName,
    Long locationId,
    String roleName
) {
}