package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.MockUser;
import io.eventuate.examples.realguardio.customerservice.customermanagement.persistence.CustomerManagementJpaPersistenceConfiguration;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.persistence.OrganizationManagementJpaPersistenceConfiguration;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.MemberService;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.OrganizationService;
import io.eventuate.examples.realguardio.customerservice.security.SecurityConfiguration;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerServiceTestData.*;
import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.SecurityTestHelper.withMockUser;
import static io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier.uniquify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CustomerServiceSecurityTest.Config.class)
public class CustomerServiceSecurityTest {

    @Configuration
    @Import({SecurityConfiguration.class, CustomerManagementJpaPersistenceConfiguration.class, OrganizationManagementJpaPersistenceConfiguration.class,
            CustomerService.class, OrganizationService.class, MemberService.class})
    @EnableAutoConfiguration
    static public class Config {
    }

    @Autowired
    private CustomerService customerService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomerEventPublisher customerEventPublisher;

    @MockitoBean
    private CustomerActionAuthorizer customerActionAuthorizer;

    @Test
    void shouldDenyCreateCustomerWithoutAuthentication() {
        assertThatThrownBy(() -> {
            customerService.createCustomer(TEST_CUSTOMER_NAME, ADMIN_PERSON);
        }).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "REALGUARDIO_CUSTOMER_EMPLOYEE")
    void shouldDenyCreateCustomerWithWrongRole() {
        assertThatThrownBy(() -> {
            customerService.createCustomer(TEST_CUSTOMER_NAME, ADMIN_PERSON);
        }).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin@realguard.io", roles = "REALGUARDIO_ADMIN")
    void shouldAllowCreateCustomerWithAdminRole() {
        CustomerAndCustomerEmployee result = customerService.createCustomer(
            TEST_CUSTOMER_NAME, uniquify(INITIAL_ADMIN));
        
        assertThat(result).isNotNull();
        assertThat(result.customer().getName()).isEqualTo(TEST_CUSTOMER_NAME);
        
        // Verify admin has COMPANY_ROLE_ADMIN
        Set<String> roles = customerService.getCustomerEmployeeRoles(
            result.customer().getId(),
            result.initialAdministrator().getId());
        assertThat(roles).contains(RolesAndPermissions.Roles.COMPANY_ROLE_ADMIN);
    }

    @Test
    void shouldDenyCreateCustomerEmployeeWithoutAuthentication() {
        assertThatThrownBy(() -> {
            customerService.createCustomerEmployee(
                NONEXISTENT_CUSTOMER_ID, JOHN_DOE);
        }).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "REALGUARDIO_ADMIN")
    void shouldDenyCreateCustomerEmployeeWithAdminRole() {
        assertThatThrownBy(() -> {
            customerService.createCustomerEmployee(
                NONEXISTENT_CUSTOMER_ID, JOHN_DOE);
        }).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldAllowCreateCustomerEmployeeWithCustomerEmployeeRole() {
        // Step 1: Create customer as admin
        PersonDetails initialAdmin = uniquify(INITIAL_ADMIN);
        CustomerAndCustomerEmployee customerResult = withMockUser(
            REALGUARDIO_ADMIN_USER,
            () -> customerService.createCustomer(
                TEST_COMPANY_NAME, initialAdmin));
        
        // Step 2: Create employee with customer employee role
        CustomerEmployee employee = withMockUser(
            new MockUser(initialAdmin.emailAddress().email(), ROLE_CUSTOMER_EMPLOYEE),
            () -> customerService.createCustomerEmployee(
                customerResult.customer().getId(), uniquify(JOHN_DOE)));
        
        assertThat(employee).isNotNull();
        assertThat(employee.getCustomerId()).isEqualTo(customerResult.customer().getId());
    }

}