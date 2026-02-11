package io.eventuate.examples.realguardio.securitysystemservice.locationroles.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.domain.LocationRolesReplicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = LocationRolesControllerTest.TestConfig.class)
public class LocationRolesControllerTest {

    @Configuration
    @ComponentScan(basePackageClasses = LocationRolesController.class)
    static class TestConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationRolesReplicaService locationRolesReplicaService;

    @Test
    public void shouldReturnLocationRoles() throws Exception {
        // Given
        String userName = "john.doe@example.com";
        Long locationId = 123L;
        LocationRole role = new LocationRole(1L, userName, locationId, RolesAndPermissions.SECURITY_SYSTEM_ARMER);
        
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
            .andExpect(jsonPath("$[0].roleName").value(RolesAndPermissions.SECURITY_SYSTEM_ARMER));
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