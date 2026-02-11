package io.eventuate.examples.realguardio.securitysystemservice.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SecuritySystemController.class)
class SecuritySystemControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecuritySystemService securitySystemService;

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .build();
        }
    }

    @Test
    @WithAnonymousUser
    void testRequestWithoutAuthenticationReturns403() throws Exception {
        mockMvc.perform(put("/securitysystems/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DISARM\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REALGUARDIO_ADMIN")
    void testRequestWithRealguardioAdminRoleSucceeds() throws Exception {
        SecuritySystem system = new SecuritySystem();
        system.setId(1L);
        system.setState(SecuritySystemState.DISARMED);
        when(securitySystemService.disarm(1L)).thenReturn(system);

        mockMvc.perform(put("/securitysystems/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DISARM\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "REALGUARDIO_CUSTOMER_EMPLOYEE")
    void testRequestWithRealguardioCustomerEmployeeRoleSucceeds() throws Exception {
        SecuritySystem system = new SecuritySystem();
        system.setId(1L);
        system.setState(SecuritySystemState.DISARMED);
        when(securitySystemService.disarm(1L)).thenReturn(system);

        mockMvc.perform(put("/securitysystems/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DISARM\"}"))
                .andExpect(status().isOk());
    }
}