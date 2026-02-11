package io.eventuate.examples.realguardio.customerservice.customermanagement;

import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerAndCustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.SecurityTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static io.eventuate.examples.realguardio.customerservice.customermanagement.common.Uniquifier.uniquify;
import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerServiceTestData.*;
import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.SecurityTestHelper.withMockUser;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CustomerServiceSecurityIntegrationTest {
    
    @TestConfiguration
    public static class TestConfig {
        @Bean
        public SecurityTestHelper securityTestHelper(AuthenticationManager authenticationManager) {
            return new SecurityTestHelper(authenticationManager);
        }
    }

    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private SecurityTestHelper securityTestHelper;
    
    @Test
    void shouldCreateCustomerThenLoginAsInitialAdminAndCreateEmployee() {
        PersonDetails initialAdmin = uniquify(INITIAL_ADMIN);
        PersonDetails newEmployee = uniquify(JOHN_DOE);
        
        // Step 1: Create a customer with initial admin using mock admin authentication
        CustomerAndCustomerEmployee result = withMockUser(
            REALGUARDIO_ADMIN_USER,
            () -> customerService.createCustomer(TEST_COMPANY_NAME, initialAdmin));
        
        assertThat(result).isNotNull();
        assertThat(result.customer().getName()).isEqualTo(TEST_COMPANY_NAME);
        assertThat(result.initialAdministrator()).isNotNull();
        
        Long customerId = result.customer().getId();
        
        // Step 2: Authenticate as the newly created initial admin and create an employee
        // Note: Using default password "changeme" that UserService sets for new users
        CustomerEmployee newCustomerEmployee = securityTestHelper.withRealUser(
            initialAdmin.emailAddress().email(),
            "changeme",
            () -> customerService.createCustomerEmployee(customerId, newEmployee));
        
        assertThat(newCustomerEmployee).isNotNull();
        assertThat(newCustomerEmployee.getCustomerId()).isEqualTo(customerId);
        
        // Verify the new employee user was created in the security system
        UserDetails newEmployeeUser = userDetailsService.loadUserByUsername(
            newEmployee.emailAddress().email());
        
        assertThat(newEmployeeUser).isNotNull();
        assertThat(newEmployeeUser.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList())
            .contains("ROLE_REALGUARDIO_CUSTOMER_EMPLOYEE");
    }
}