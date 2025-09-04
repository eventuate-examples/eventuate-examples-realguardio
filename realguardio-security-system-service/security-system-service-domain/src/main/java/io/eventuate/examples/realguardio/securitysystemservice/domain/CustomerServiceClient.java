package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Set;

public interface CustomerServiceClient {
    Set<String> getUserRolesAtLocation(String userId, Long locationId);
    Set<String> getUserRolesAtLocation(String userId, Long locationId, String jwtToken);
}