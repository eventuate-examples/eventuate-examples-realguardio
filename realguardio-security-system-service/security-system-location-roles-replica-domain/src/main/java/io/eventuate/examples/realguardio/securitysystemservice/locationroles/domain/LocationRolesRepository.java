package io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain;

import java.util.List;

public interface LocationRolesRepository {

    void saveLocationRole(String userName, Long locationId, String roleName);

    void saveTeamMember(String teamId, String customerEmployeeId);

    void saveTeamLocationRole(String teamId, String roleName, Long locationId);

    void saveLocation(Long locationId, String customerId);

    List<LocationRole> findLocationRoles(String userName, Long locationId);
}
