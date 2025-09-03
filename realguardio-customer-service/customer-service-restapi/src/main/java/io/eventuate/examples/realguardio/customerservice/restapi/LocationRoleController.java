package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
            @PathVariable("locationId") Long locationId,
            Authentication authentication) {
        
        // For now, use a default user ID when authentication is not present (for testing)
        String userId = authentication != null ? authentication.getName() : "123";
        
        Set<String> roles = locationRoleService.getUserRolesAtLocation(userId, locationId);
        
        if (roles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(new RolesResponse(roles));
    }
}