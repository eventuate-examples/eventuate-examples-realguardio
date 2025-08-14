package io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport;

import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class LoggedInUser {
  private final UserNameSupplier userNameSupplier;
  private boolean userSet;

  public LoggedInUser() {
    this.userNameSupplier = mock(UserNameSupplier.class);
    this.userSet = false;
  }

  void withUser(String username) {
    assertThat(userSet).isFalse();
    System.out.println("Setting user: " + username);
    when(userNameSupplier.getCurrentUserEmail()).thenReturn(username);
    userSet = true;
  }

  public void withoutUser() {
    reset(userNameSupplier);
    userSet = false;
  }

  public UserNameSupplier getUserNameSupplier() {
    return userNameSupplier;
  }

  public void withUser(MockUser user) {
    withUser(user.username());
  }


  <T> T doWithUser(PersonDetails username, Supplier<T> supplier) {
    return doWithUser(username.emailAddress().email(), supplier);
  }

  <T> T doWithUser(String username, Supplier<T> supplier) {
    if (userSet) {
      return supplier.get();
    } else {
      withUser(username);
      try {
        return supplier.get();
      } finally {
        withoutUser();
      }
    }
  }

  <T> T doWithUser(MockUser user, Supplier<T> supplier) {
    return doWithUser(user.username(), supplier);
  }


  public void withUser(PersonDetails user) {
    withUser(user.emailAddress().email());
  }

  public void withUser(TestCustomerEmployee employee) {
    withUser(employee.employeeDetails());
  }

  public void withUser(TestCustomer customer) {
    withUser(customer.initialAdminDetails());
  }
}
