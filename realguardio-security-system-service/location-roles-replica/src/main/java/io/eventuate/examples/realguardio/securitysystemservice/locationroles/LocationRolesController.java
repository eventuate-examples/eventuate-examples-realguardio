package io.eventuate.examples.realguardio.securitysystemservice.locationroles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/location-roles")
public class LocationRolesController {
    
    private final LocationRolesReplicaService locationRolesReplicaService;
    
    @Autowired
    public LocationRolesController(LocationRolesReplicaService locationRolesReplicaService) {
        this.locationRolesReplicaService = locationRolesReplicaService;
    }
    
    @GetMapping
    public List<LocationRole> getLocationRoles(
            @RequestParam("userName") String userName,
            @RequestParam("locationId") Long locationId) {
        return locationRolesReplicaService.findLocationRoles(userName, locationId);
    }
}