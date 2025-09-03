package io.eventuate.examples.realguardio.securitysystemservice.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.NotFoundException;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SecuritySystemController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class SecuritySystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecuritySystemService securitySystemService;

    @Test
    void shouldReturnSecuritySystems() throws Exception {
        SecuritySystem system1 = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(system1, 1L);
        
        SecuritySystem system2 = new SecuritySystem("Office Back Door", SecuritySystemState.DISARMED,
                new HashSet<>());
        setId(system2, 2L);
        
        List<SecuritySystem> systems = Arrays.asList(system1, system2);
        
        when(securitySystemService.findAll()).thenReturn(systems);
        
        mockMvc.perform(get("/securitysystems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securitySystems[0].id").value(1))
                .andExpect(jsonPath("$.securitySystems[0].locationName").value("Office Front Door"))
                .andExpect(jsonPath("$.securitySystems[0].state").value("ARMED"))
                .andExpect(jsonPath("$.securitySystems[0].actions[0]").value("ARM"))
                .andExpect(jsonPath("$.securitySystems[1].id").value(2))
                .andExpect(jsonPath("$.securitySystems[1].locationName").value("Office Back Door"))
                .andExpect(jsonPath("$.securitySystems[1].state").value("DISARMED"));
    }
    
    private void setId(SecuritySystem system, Long id) throws Exception {
        Field idField = SecuritySystem.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(system, id);
    }

    @Test
    void shouldDisarmSecuritySystem() throws Exception {
        Long systemId = 1L;
        SecuritySystem disarmedSystem = new SecuritySystem("Office Front Door", SecuritySystemState.DISARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.ARM)));
        setId(disarmedSystem, systemId);
        disarmedSystem.setLocationId(456L);
        
        when(securitySystemService.disarm(systemId)).thenReturn(disarmedSystem);
        
        String requestBody = "{\"action\": \"DISARM\"}";
        
        mockMvc.perform(put("/securitysystems/{id}", systemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.locationName").value("Office Front Door"))
                .andExpect(jsonPath("$.state").value("DISARMED"))
                .andExpect(jsonPath("$.locationId").value(456))
                .andExpect(jsonPath("$.actions[0]").value("ARM"));
    }

    @Test
    void shouldArmSecuritySystem() throws Exception {
        Long systemId = 1L;
        SecuritySystem armedSystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED,
                new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM)));
        setId(armedSystem, systemId);
        armedSystem.setLocationId(456L);
        
        when(securitySystemService.arm(systemId)).thenReturn(armedSystem);
        
        String requestBody = "{\"action\": \"ARM\"}";
        
        mockMvc.perform(put("/securitysystems/{id}", systemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.locationName").value("Office Front Door"))
                .andExpect(jsonPath("$.state").value("ARMED"))
                .andExpect(jsonPath("$.locationId").value(456))
                .andExpect(jsonPath("$.actions[0]").value("DISARM"));
    }

    @Test
    void shouldReturnBadRequestForInvalidAction() throws Exception {
        Long systemId = 1L;
        
        String requestBody = "{\"action\": \"ACKNOWLEDGE\"}";
        
        mockMvc.perform(put("/securitysystems/{id}", systemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

}