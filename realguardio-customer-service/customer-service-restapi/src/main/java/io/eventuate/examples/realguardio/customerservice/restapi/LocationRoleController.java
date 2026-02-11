package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class LocationRoleController {
    
    private final LocationRoleService locationRoleService;
    
    public LocationRoleController(LocationRoleService locationRoleService) {
        this.locationRoleService = locationRoleService;
    }
    
    @GetMapping("/locations/{locationId}/roles")
    @PreAuthorize("hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE') or hasRole('REALGUARDIO_ADMIN')")
    public ResponseEntity<RolesResponse> getUserRolesAtLocation(
            @PathVariable("locationId") Long locationId) {
        
        Set<String> roles = locationRoleService.getUserRolesAtLocation(locationId);
        
        return ResponseEntity.ok(new RolesResponse(roles));
    }
}