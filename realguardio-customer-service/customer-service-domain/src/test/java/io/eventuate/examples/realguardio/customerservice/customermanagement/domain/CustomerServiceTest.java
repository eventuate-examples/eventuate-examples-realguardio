package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.LoggedInUser;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.TestContext;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.TestCustomerFactory;
import io.eventuate.examples.realguardio.customerservice.customermanagement.persistence.CustomerManagementJpaPersistenceConfiguration;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.exception.NotAuthorizedException;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.persistence.OrganizationManagementJpaPersistenceConfiguration;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.MemberService;
import io.eventuate.examples.realguardio.customerservice.organizationmanagement.service.OrganizationService;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerServiceTestData.*;
import static io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier.uniquify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = CustomerServiceTest.Config.class)
class CustomerServiceTest {

  @Configuration
  @Import({CustomerManagementJpaPersistenceConfiguration.class, OrganizationManagementJpaPersistenceConfiguration.class,
          CustomerService.class, OrganizationService.class, MemberService.class})
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

  @MockitoBean
  private CustomerEventPublisher customerEventPublisher;

  @MockitoBean
  private CustomerActionAuthorizer customerActionAuthorizer;

  @Autowired
  private LoggedInUser loggedInUser;

  @Autowired
  private TestCustomerFactory testCustomerFactory;

  @Autowired
  private CustomerService customerService;

  @Autowired
  private UserNameSupplier userNameSupplier;

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

    createdCustomer.assertThatCustomerEmployeeRoles(initialAdmin).containsExactly(RolesAndPermissions.COMPANY_ROLE_ADMIN);
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

    long idOfNonExistentCustomer = System.currentTimeMillis();

    // When & Then

    assertThatThrownBy(() ->
        customerService.assignRole(idOfNonExistentCustomer, johnDoe.customerEmployee().getId(), SECURITY_SYSTEM_DISARMER_ROLE))
        .isInstanceOf(DataRetrievalFailureException.class);
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

    verify(customerActionAuthorizer).verifyCanDo(customer.customer().getId(), RolesAndPermissions.CREATE_CUSTOMER_EMPLOYEE);

  }

  @Test
  public void shouldFailWhenNotAuthorized() {

    // Given

    var customer1 = testCustomerFactory.createCustomer();
    var customer2 = testCustomerFactory.createCustomer();

    loggedInUser.withUser(customer1);

    doThrow(new NotAuthorizedException("message")).when(customerActionAuthorizer).verifyCanDo(customer1.customer().getId(), RolesAndPermissions.CREATE_CUSTOMER_EMPLOYEE);

    // When & Then

    assertThatThrownBy(() -> {
        loggedInUser.doWithUser(customer2.initialAdminDetails(),
            () -> customerService.createCustomerEmployee(customer1.customer().getId(), uniquify(JOHN_DOE_DETAILS)));
    }).isInstanceOf(NotAuthorizedException.class) ;
  }

  @Test
  public void shouldPublishEventWhenAssigningLocationRole() {
    // Given
    var customer = testCustomerFactory.createCustomer();
    var marySmith = customer.createCustomerEmployee();
    var location = customer.createLocation();
    loggedInUser.withUser(customer);

    // Clear any previous interactions
    Mockito.clearInvocations(customerEventPublisher);

    // When
    customer.assignLocationRole(marySmith, location, SECURITY_SYSTEM_ARMER_ROLE);

    // Then - verify the role was assigned
    customer.assertEmployeeLocationRoles(marySmith, location).containsExactly(SECURITY_SYSTEM_ARMER_ROLE);

    CustomerEmployeeAssignedLocationRole expectedEvent =
        new CustomerEmployeeAssignedLocationRole(
            marySmith.employeeDetails().emailAddress().email(),
            location.getId(),
            SECURITY_SYSTEM_ARMER_ROLE
        );

    verify(customerEventPublisher).publish(
        any(Customer.class),
        eq(expectedEvent)
    );
  }

  @Test
  public void shouldPublishTeamMemberAddedEventWhenAddingTeamMember() {
    // Given
    var customer = testCustomerFactory.createCustomer();
    var team = customer.createTeam("Operations Team");
    var employee = customer.createCustomerEmployee();

    Mockito.clearInvocations(customerEventPublisher);

    // When
    customer.addTeamMember(team.getId(), employee.customerEmployee().getId());

    // Then
    TeamMemberAdded expectedEvent = new TeamMemberAdded(
        team.getId(),
        employee.customerEmployee().getId()
    );

    verify(customerEventPublisher).publish(
        any(Customer.class),
        eq(expectedEvent)
    );
  }

  @Test
  public void shouldPublishTeamAssignedLocationRoleEventWhenAssigningTeamRole() {
    // Given
    var customer = testCustomerFactory.createCustomer();
    var team = customer.createTeam("Security Team");
    var location = customer.createLocation();

    Mockito.clearInvocations(customerEventPublisher);

    // When
    customerService.assignTeamRole(team.getId(), location.getId(), SECURITY_SYSTEM_DISARMER_ROLE);

    // Then
    TeamAssignedLocationRole expectedEvent = new TeamAssignedLocationRole(
        team.getId(),
        location.getId(),
        SECURITY_SYSTEM_DISARMER_ROLE
    );

    verify(customerEventPublisher).publish(
        any(Customer.class),
        eq(expectedEvent)
    );
  }


}
