package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
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
    private CustomerService customerService;

    private static final String CREATE_LOCATION_REQUEST_JSON = """
        {
            "name": "Main Office"
        }
        """;

    @Test
    @WithMockUser(roles = "REALGUARDIO_CUSTOMER_EMPLOYEE")
    void shouldCreateLocation() throws Exception {
        long customerId = 10L;
        long locationId = 100L;
        String locationName = "Main Office";

        Location location = new Location(locationName, customerId);
        EntityUtil.setId(location, locationId);

        when(customerService.createLocationForCustomer(anyLong(), anyString())).thenReturn(location);

        mockMvc.perform(post("/customers/{customerId}/locations", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_LOCATION_REQUEST_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId").value(locationId));
    }
}
