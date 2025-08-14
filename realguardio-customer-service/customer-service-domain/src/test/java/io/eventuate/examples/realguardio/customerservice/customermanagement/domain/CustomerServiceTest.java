package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.customermanagement.CustomerManagementConfiguration;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.LoggedInUser;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.TestContext;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.TestCustomerFactory;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.exception.NotAuthorizedException;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerServiceTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CustomerServiceTest.Config.class)
class CustomerServiceTest {

  @Configuration
  @Import(CustomerManagementConfiguration.class)
  @EnableAutoConfiguration
  public static class Config {

    @Bean
    LoggedInUser loggedInUser() {
      return new LoggedInUser();
    }

    @Bean
    public UserNameSupplier userNameSupplier() {
      return loggedInUser().getUserNameSupplier();
    }

    @Bean
    public TestContext testContext() {
      return new TestContext();
    }

    @Bean
    TestCustomerFactory testCustomerFactory() {
      return new TestCustomerFactory();
    }
  }

  @MockitoBean
  private UserService userService;

  @Autowired
  private LoggedInUser loggedInUser;

  @Autowired
  private TestCustomerFactory testCustomerFactory;

  @Autowired
  private CustomerService customerService;

  @BeforeEach
  public void setUp() {
    loggedInUser.withoutUser();
  }

  @Test
  void shouldCreateCustomer() {
    // Given

    loggedInUser.withUser(REALGUARDIO_ADMIN_USER);

    String customerName = uniqueCustomerName();
    PersonDetails initialAdminDetails = INITIAL_ADMIN;

    // When

    var createdCustomer = testCustomerFactory.createCustomer(customerName, initialAdminDetails);

    // Then

    var customer = createdCustomer.customer();
    assertThat(customer.getName()).isEqualTo(customerName);

    CustomerEmployee initialAdmin = createdCustomer.initialAdministrator();
    assertThat(initialAdmin.getCustomerId()).isEqualTo(customer.getId());

    PersonDetails retrievedAdminDetails = createdCustomer.findEmployeeDetails(initialAdmin);
    assertThat(retrievedAdminDetails).isEqualTo(initialAdminDetails);

    createdCustomer.assertThatCustomerEmployeeRoles(initialAdmin).containsExactly(CustomerService.COMPANY_ROLE_ADMIN);
  }


  @Test
  public void shouldCreateCustomerEmployee() {

    // Given

    var customer = testCustomerFactory.createCustomer();

    loggedInUser.withUser(customer);

    // When

    var johnDoe = customer.createCustomerEmployee(JOHN_DOE_DETAILS);

    // Then

    assertThat(johnDoe).isNotNull();
    assertThat(johnDoe.customerEmployee().getCustomerId()).isEqualTo(customer.customer().getId());

    PersonDetails retrievedJohnDetails = customer.findEmployeeDetails(johnDoe);
    assertThat(retrievedJohnDetails).isEqualTo(JOHN_DOE_DETAILS);

    customer.assertThatCustomerEmployeeRoles(johnDoe).isEmpty();
  }


  @Test
  public void shouldAssignCustomerEmployeeRoles() {

    // Given

    var customer = testCustomerFactory.createCustomer();

    var johnDoe = customer.createCustomerEmployee();

    loggedInUser.withUser(customer);

    // When

    customer.assignRole(johnDoe, SECURITY_SYSTEM_DISARMER_ROLE);

    // Then

    customer.assertThatCustomerEmployeeRoles(johnDoe).containsExactly(SECURITY_SYSTEM_DISARMER_ROLE);
  }

  @Test
  public void shouldFailToAssignCustomerEmployeeRolesForNonExistentCustomer() {

    // Given

    var customer = testCustomerFactory.createCustomer();

    var johnDoe = customer.createCustomerEmployee();

    loggedInUser.withUser(customer);

    // When & Then

    assertThatThrownBy(() -> 
        customerService.assignRole(System.currentTimeMillis(), johnDoe.customerEmployee().getId(), SECURITY_SYSTEM_DISARMER_ROLE))
        .isInstanceOf(DataRetrievalFailureException.class);
  }

  @Test
  void shouldDenyCreateCustomerEmployeeWhenCallerIsNotAdmin() {

    // Given

    var customer = testCustomerFactory.createCustomer();

    var johnDoe = customer.createCustomerEmployee();

    loggedInUser.withUser(johnDoe);

    // When & Then

    assertThatThrownBy(() -> customer.createCustomerEmployee(NEW_EMPLOYEE))
        .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void shouldRequireCompanyAdminToAssignRoles() {

    // Given

    var customer = testCustomerFactory.createCustomer();

    var johnDoe = customer.createCustomerEmployee();

    var patrick = customer.createCustomerEmployee();

    loggedInUser.withUser(johnDoe);

    // When

    assertThatThrownBy(() -> customer.assignRole(patrick, SECURITY_SYSTEM_DISARMER_ROLE))
        .isInstanceOf(NotAuthorizedException.class);

  }

  @Test
  public void shouldCreateLocationForCustomer() {

    // Given

    var customer = testCustomerFactory.createCustomer();

    // When

    var location = customer.createLocation(MAIN_OFFICE_LOCATION);

    // Then

    assertThat(location).isNotNull();
    assertThat(location.getName()).isEqualTo(MAIN_OFFICE_LOCATION);
    assertThat(location.getCustomerId()).isEqualTo(customer.customer().getId());

  }

  @Test
  public void shouldCreateCustomerEmployeeAndAssignLocationRoles() {
    // Given

    var customer = testCustomerFactory.createCustomer();

    var marySmith = customer.createCustomerEmployee();

    var location = customer.createLocation();

    loggedInUser.withUser(customer);

    // When

    customer.assignLocationRole(marySmith, location, SECURITY_SYSTEM_ARMER_ROLE);

    // Then

    customer.assertEmployeeLocationRoles(marySmith, location).containsExactly(SECURITY_SYSTEM_ARMER_ROLE);

  }

  @Test
  public void shouldBeInSameCompanyToCreateEmployee() {

    // Given

    var customer1 = testCustomerFactory.createCustomer();
    var customer2 = testCustomerFactory.createCustomer();

    loggedInUser.withUser(customer1);

    // When & Then

    assertThatThrownBy(customer2::createCustomerEmployee)
        .isInstanceOf(NotAuthorizedException.class) ;
  }


}
