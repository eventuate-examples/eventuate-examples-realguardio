package io.eventuate.examples.realguardio.securityservice.locationroles;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationRolesController.class)
public class LocationRolesControllerTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationRolesReplicaService locationRolesReplicaService;

    @Test
    public void shouldReturnLocationRoles() throws Exception {
        // Given
        String userName = "john.doe@example.com";
        Long locationId = 123L;
        LocationRole role = new LocationRole(1L, userName, locationId, "SECURITY_SYSTEM_ARMER");
        
        when(locationRolesReplicaService.findLocationRoles(userName, locationId))
            .thenReturn(List.of(role));

        // When & Then
        mockMvc.perform(get("/location-roles")
                .param("userName", userName)
                .param("locationId", locationId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].userName").value(userName))
            .andExpect(jsonPath("$[0].locationId").value(locationId))
            .andExpect(jsonPath("$[0].roleName").value("SECURITY_SYSTEM_ARMER"));
    }

    @Test
    public void shouldReturnEmptyListWhenNoRolesFound() throws Exception {
        // Given
        String userName = "unknown@example.com";
        Long locationId = 999L;
        
        when(locationRolesReplicaService.findLocationRoles(userName, locationId))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/location-roles")
                .param("userName", userName)
                .param("locationId", locationId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }
}