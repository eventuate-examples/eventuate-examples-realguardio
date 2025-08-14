package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
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
        Customer customer1 = new Customer("Acme, Inc", 1L);
        setId(customer1, 1L);
        
        Customer customer2 = new Customer("Big Co, Inc", 2L);
        setId(customer2, 2L);
        
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
    
    private void setId(Customer system, Long id) throws Exception {
        Field idField = Customer.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(system, id);
    }

}