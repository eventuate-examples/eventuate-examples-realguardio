package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerAndCustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

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

    @MockBean
    private CustomerService customerService;

    @Test
    @WithMockUser
    void shouldReturnCustomers() throws Exception {
        Customer customer1 = new Customer("Acme, Inc", 1L);
        EntityUtil.setId(customer1, 1L);
        
        Customer customer2 = new Customer("Big Co, Inc", 2L);
        EntityUtil.setId(customer2, 2L);
        
        List<Customer> customers = Arrays.asList(customer1, customer2);
        
        when(customerService.findAll()).thenReturn(customers);
        
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers[0].id").value(customer1.getId()))
                .andExpect(jsonPath("$.customers[0].name").value(customer1.getName()))
                .andExpect(jsonPath("$.customers[1].id").value(customer2.getId()))
                .andExpect(jsonPath("$.customers[1].name").value(customer2.getName()))
        ;
    }


    private static final String CREATE_CUSTOMER_REQUEST_JSON = """
        {
            "name": "New Customer",
            "initialAdministrator": {
                "name": {
                    "firstName": "Admin",
                    "lastName": "User"
                },
                "emailAddress": {
                    "email": "admin@example.com"
                }
            }
        }
        """;

    @Test
    @WithMockUser(roles = "REALGUARDIO_ADMIN")
    void shouldCreateCustomer() throws Exception {
        long employeeId = 1L;
        long organizationId = 2L;
        long memberId = 100L;
        long customerId = 10L;

        Customer customer = new Customer("New Customer", organizationId);
        EntityUtil.setId(customer, customerId);
        CustomerEmployee employee = new CustomerEmployee(customer.getId(), memberId);
        EntityUtil.setId(employee, employeeId);
        
        CustomerAndCustomerEmployee result = new CustomerAndCustomerEmployee(customer, employee);
        
        when(customerService.createCustomer(anyString(), any())).thenReturn(result);
        
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_CUSTOMER_REQUEST_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customer.id").value(customerId))
            .andExpect(jsonPath("$.customer.name").value("New Customer"))
            .andExpect(jsonPath("$.initialAdministrator.id").value(employeeId))
            .andExpect(jsonPath("$.initialAdministrator.customerId").value(customerId))
            .andExpect(jsonPath("$.initialAdministrator.memberId").value(memberId))
        ;
    }

    @Test
    @WithMockUser(roles = "REALGUARDIO_CUSTOMER_EMPLOYEE")
    void shouldNotAllowCustomerEmployeeToCreateCustomer() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_CUSTOMER_REQUEST_JSON))
            .andExpect(status().isForbidden());
    }

}