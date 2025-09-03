package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import java.util.Set;

public interface LocationRoleService {
    Set<String> getUserRolesAtLocation(String userId, Long locationId);
}