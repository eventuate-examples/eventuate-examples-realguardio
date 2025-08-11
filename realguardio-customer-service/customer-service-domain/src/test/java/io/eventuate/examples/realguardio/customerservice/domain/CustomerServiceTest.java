package io.eventuate.examples.realguardio.customerservice.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository);
    }

    @Test
    void shouldReturnAllCustomers() throws Exception {
        // Given
        Customer system1 = new Customer("Office Front Door", CustomerState.ARMED, 
                new HashSet<>(Arrays.asList(CustomerAction.ARM)));
        setId(system1, 1L);
        
        Customer system2 = new Customer("Office Back Door", CustomerState.DISARMED, 
                new HashSet<>());
        setId(system2, 2L);
        
        List<Customer> expectedSystems = Arrays.asList(system1, system2);
        when(customerRepository.findAll()).thenReturn(expectedSystems);
        
        // When
        List<Customer> actualSystems = customerService.findAll();
        
        // Then
        assertThat(actualSystems).hasSize(2);
        assertThat(actualSystems).containsExactlyElementsOf(expectedSystems);
        assertThat(actualSystems.get(0).getLocationName()).isEqualTo("Office Front Door");
        assertThat(actualSystems.get(0).getState()).isEqualTo(CustomerState.ARMED);
        assertThat(actualSystems.get(1).getLocationName()).isEqualTo("Office Back Door");
        assertThat(actualSystems.get(1).getState()).isEqualTo(CustomerState.DISARMED);
    }
    
    private void setId(Customer system, Long id) throws Exception {
        Field idField = Customer.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(system, id);
    }

    @Test
    void shouldReturnEmptyListWhenNoSystemsExist() {
        // Given
        when(customerRepository.findAll()).thenReturn(List.of());
        
        // When
        List<Customer> actualSystems = customerService.findAll();
        
        // Then
        assertThat(actualSystems).isEmpty();
    }
}