package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class LocationRoleServiceImpl implements LocationRoleService {
    
    private final CustomerEmployeeLocationRoleRepository locationRoleRepository;
    private final TeamLocationRoleRepository teamLocationRoleRepository;
    private final LocationRepository locationRepository;
    private final UserNameSupplier userNameSupplier;

  public LocationRoleServiceImpl(CustomerEmployeeLocationRoleRepository locationRoleRepository,
                                   TeamLocationRoleRepository teamLocationRoleRepository,
                                   LocationRepository locationRepository,
                                   UserNameSupplier userNameSupplier) {
        this.locationRoleRepository = locationRoleRepository;
        this.teamLocationRoleRepository = teamLocationRoleRepository;
        this.locationRepository = locationRepository;
    this.userNameSupplier = userNameSupplier;
  }
    
    @Override
    public Set<String> getUserRolesAtLocation(Long locationId) {

        String userName = userNameSupplier.getCurrentUserEmail();

        Set<String> allRoles = new HashSet<>();

        Set<String> directRoles = findDirectRolesForEmployeeAtLocation(userName, locationId);
        allRoles.addAll(directRoles);

        Set<String> teamRoles = findTeamRolesForEmployeeAtLocation(userName, locationId);
        allRoles.addAll(teamRoles);
        
        return allRoles;
    }
    
    private Set<String> findDirectRolesForEmployeeAtLocation(String userName, Long locationId) {
        return new HashSet<>(locationRoleRepository.findRoleNamesByUserNameAndLocationId(
                userName, locationId));
    }
    
    private Set<String> findTeamRolesForEmployeeAtLocation(String userName, Long locationId) {
        // TODO: Implement team-based role retrieval when TeamLocationRoleRepository has appropriate query methods
        return Set.of();
    }
}