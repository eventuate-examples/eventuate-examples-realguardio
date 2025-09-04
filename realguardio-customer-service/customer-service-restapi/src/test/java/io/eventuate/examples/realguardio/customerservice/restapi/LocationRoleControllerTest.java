package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LocationRoleController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class LocationRoleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LocationRoleService locationRoleService;
    
    @Test
    void shouldReturnRolesForUserAtLocation() throws Exception {
        Long locationId = 456L;
        String userId = "123";
        Set<String> expectedRoles = Set.of("CAN_ARM", "CAN_DISARM", "VIEW_ALERTS");
        
        when(locationRoleService.getUserRolesAtLocation(locationId))
            .thenReturn(expectedRoles);
        
        mockMvc.perform(get("/locations/{locationId}/roles", locationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[0]").exists())
            .andExpect(jsonPath("$.roles.length()").value(3));
    }
    
    @Test
    void shouldReturnEmptySetWhenNoRolesFound() throws Exception {
        Long locationId = 999L;
        String userId = "123";
        
        when(locationRoleService.getUserRolesAtLocation(locationId))
            .thenReturn(Set.of());
        
        mockMvc.perform(get("/locations/{locationId}/roles", locationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles").isEmpty());
    }
}