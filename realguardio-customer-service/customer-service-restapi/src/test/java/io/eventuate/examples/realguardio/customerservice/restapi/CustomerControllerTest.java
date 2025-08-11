package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerAction;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CustomerController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Test
    void shouldReturnCustomers() throws Exception {
        Customer system1 = new Customer("Office Front Door", CustomerState.ARMED,
                new HashSet<>(Arrays.asList(CustomerAction.ARM)));
        setId(system1, 1L);
        
        Customer system2 = new Customer("Office Back Door", CustomerState.DISARMED,
                new HashSet<>());
        setId(system2, 2L);
        
        List<Customer> systems = Arrays.asList(system1, system2);
        
        when(customerService.findAll()).thenReturn(systems);
        
        mockMvc.perform(get("/securitysystems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customers[0].id").value(1))
                .andExpect(jsonPath("$.customers[0].locationName").value("Office Front Door"))
                .andExpect(jsonPath("$.customers[0].state").value("ARMED"))
                .andExpect(jsonPath("$.customers[0].actions[0]").value("ARM"))
                .andExpect(jsonPath("$.customers[1].id").value(2))
                .andExpect(jsonPath("$.customers[1].locationName").value("Office Back Door"))
                .andExpect(jsonPath("$.customers[1].state").value("DISARMED"));
    }
    
    private void setId(Customer system, Long id) throws Exception {
        Field idField = Customer.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(system, id);
    }

}