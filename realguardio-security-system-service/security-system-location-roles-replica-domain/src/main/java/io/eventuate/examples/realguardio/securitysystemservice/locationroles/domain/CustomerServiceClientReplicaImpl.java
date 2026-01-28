package io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain;

import io.eventuate.examples.realguardio.securitysystemservice.domain.CustomerServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomerServiceClientReplicaImpl implements CustomerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClientReplicaImpl.class);

    private final LocationRolesReplicaService locationRolesReplicaService;

    public CustomerServiceClientReplicaImpl(LocationRolesReplicaService locationRolesReplicaService) {
        this.locationRolesReplicaService = locationRolesReplicaService;
    }

    @Override
    public Set<String> getUserRolesAtLocation(String userId, Long locationId) {
        logger.info("Retrieving roles for user {} at location {} from replica", userId, locationId);
        
        List<LocationRole> locationRoles = locationRolesReplicaService.findLocationRoles(userId, locationId);
        
        Set<String> roles = locationRoles.stream()
            .map(LocationRole::roleName)
            .collect(Collectors.toSet());
        
        logger.info("Retrieved roles for user {} at location {} from replica: {}", 
            userId, locationId, roles);
        
        return roles;
    }
}