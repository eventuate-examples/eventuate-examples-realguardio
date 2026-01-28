package io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain;

import java.util.List;

public class LocationRolesReplicaService {

    private final LocationRolesRepository locationRolesRepository;

    public LocationRolesReplicaService(LocationRolesRepository locationRolesRepository) {
        this.locationRolesRepository = locationRolesRepository;
    }

    public void saveLocationRole(String userName, Long locationId, String roleName) {
        locationRolesRepository.saveLocationRole(userName, locationId, roleName);
    }

    public void saveTeamMember(String teamId, String customerEmployeeId) {
        locationRolesRepository.saveTeamMember(teamId, customerEmployeeId);
    }

    public void saveTeamLocationRole(String teamId, String roleName, Long locationId) {
        locationRolesRepository.saveTeamLocationRole(teamId, roleName, locationId);
    }

    public void saveLocation(Long locationId, String customerId) {
        locationRolesRepository.saveLocation(locationId, customerId);
    }

    public List<LocationRole> findLocationRoles(String userName, Long locationId) {
        return locationRolesRepository.findLocationRoles(userName, locationId);
    }
}