package io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport;

import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerAndCustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Location;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.ObjectAssert;

import java.util.Collection;
import java.util.Set;

import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerServiceTestData.JOHN_DOE_DETAILS;
import static io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier.uniquify;
import static org.assertj.core.api.Assertions.assertThat;

public record TestCustomer(TestContext testContext,
                           CustomerAndCustomerEmployee customerAndInitialAdmin, PersonDetails initialAdminDetails) {

  public Customer customer() {
    return customerAndInitialAdmin.customer();
  }

  public CustomerEmployee initialAdministrator() {
    return customerAndInitialAdmin.initialAdministrator();
  }

  public PersonDetails findEmployeeDetails(CustomerEmployee initialAdmin) {
    return testContext.customerService.findEmployeeDetails(customer().getId(), initialAdmin.getId());
  }

  public AbstractCollectionAssert<? extends AbstractCollectionAssert<?, Collection<? extends String>, String, ObjectAssert<String>>, Collection<? extends String>, String, ObjectAssert<String>>
  assertThatCustomerEmployeeRoles(CustomerEmployee customerEmployee) {
    Set<String> roles = testContext.customerService.getCustomerEmployeeRoles(customer().getId(), customerEmployee.getId());
    return assertThat(roles);
  }

  public AbstractCollectionAssert<? extends AbstractCollectionAssert<?, Collection<? extends String>, String, ObjectAssert<String>>, Collection<? extends String>, String, ObjectAssert<String>>
    assertThatCustomerEmployeeRoles(TestCustomerEmployee employee) {
    return assertThatCustomerEmployeeRoles(employee.customerEmployee());
  }
  public TestCustomerEmployee createCustomerEmployee() {
    PersonDetails employeeDetails = uniquify(JOHN_DOE_DETAILS);
    return createCustomerEmployee(employeeDetails);
  }

  public TestCustomerEmployee createCustomerEmployee(PersonDetails employeeDetails) {
    CustomerEmployee customerEmployee = testContext.userNameSupplier.doWithUser(initialAdminDetails(),
        () -> testContext.customerService.createCustomerEmployee(customer().getId(), employeeDetails));
    return new TestCustomerEmployee(customerEmployee, employeeDetails);
  }


  public PersonDetails findEmployeeDetails(TestCustomerEmployee employee) {
    return findEmployeeDetails(employee.customerEmployee());
  }

  public void assignRole(TestCustomerEmployee employee, String role) {
    testContext.customerService.assignRole(customer().getId(), employee.customerEmployee().getId(), role);
  }

  public Location createLocation() {
    return createLocation(uniquify("Test Location"));
  }

  public Location createLocation(String locationName) {
    return testContext.customerService.createLocation(customer().getId(), locationName);
  }

  public void assignLocationRole(TestCustomerEmployee employee, Location location, String role) {
    testContext.customerService.assignLocationRole(customer().getId(), employee.customerEmployee().getId(), location.getId(), role);
  }

  public AbstractCollectionAssert<?, Collection<? extends String>, String, ObjectAssert<String>>
  assertEmployeeLocationRoles(TestCustomerEmployee employee, Location location) {
    return assertThat(testContext.customerService.getCustomerEmployeeLocationRoles(
        customer().getId(), employee.customerEmployee().getId(), location.getId()));
  }
  
}
