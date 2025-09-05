package io.eventuate.examples.realguardio.securityservice.locationroles;

public record LocationRole(
    Long id,
    String userName,
    Long locationId,
    String roleName
) {
}