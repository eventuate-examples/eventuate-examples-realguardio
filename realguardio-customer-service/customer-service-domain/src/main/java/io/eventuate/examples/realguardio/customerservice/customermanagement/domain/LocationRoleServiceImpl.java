package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class LocationRoleServiceImpl implements LocationRoleService {
    
    private final CustomerEmployeeLocationRoleRepository locationRoleRepository;
    private final TeamLocationRoleRepository teamLocationRoleRepository;
    private final LocationRepository locationRepository;
    
    public LocationRoleServiceImpl(CustomerEmployeeLocationRoleRepository locationRoleRepository,
                                   TeamLocationRoleRepository teamLocationRoleRepository,
                                   LocationRepository locationRepository) {
        this.locationRoleRepository = locationRoleRepository;
        this.teamLocationRoleRepository = teamLocationRoleRepository;
        this.locationRepository = locationRepository;
    }
    
    @Override
    public Set<String> getUserRolesAtLocation(String userId, Long locationId) {
        Set<String> allRoles = new HashSet<>();
        
        try {
            Long employeeId = Long.parseLong(userId);
            
            // Get direct location roles
            Set<String> directRoles = findDirectRolesForEmployeeAtLocation(employeeId, locationId);
            allRoles.addAll(directRoles);
            
            // Get team-based location roles
            Set<String> teamRoles = findTeamRolesForEmployeeAtLocation(employeeId, locationId);
            allRoles.addAll(teamRoles);
            
        } catch (NumberFormatException e) {
            // Invalid user ID format, return empty set
        }
        
        return allRoles;
    }
    
    private Set<String> findDirectRolesForEmployeeAtLocation(Long employeeId, Long locationId) {
        Optional<Location> location = locationRepository.findById(locationId);
        if (location.isEmpty()) {
            return Set.of();
        }
        
        Long customerId = location.get().getCustomerId();
        return new HashSet<>(locationRoleRepository.findRoleNamesByCustomerIdAndEmployeeIdAndLocationId(
                customerId, employeeId, locationId));
    }
    
    private Set<String> findTeamRolesForEmployeeAtLocation(Long employeeId, Long locationId) {
        // TODO: Implement team-based role retrieval when TeamLocationRoleRepository has appropriate query methods
        return Set.of();
    }
}