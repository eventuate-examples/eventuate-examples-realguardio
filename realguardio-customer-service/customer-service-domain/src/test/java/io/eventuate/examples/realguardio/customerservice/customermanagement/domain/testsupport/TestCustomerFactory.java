package io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerAndCustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;
import org.springframework.beans.factory.annotation.Autowired;

import static io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerServiceTestData.*;
import static io.eventuate.examples.realguardio.customerservice.customermanagement.common.Uniquifier.uniquify;

public class TestCustomerFactory {


  @Autowired
  private TestContext testContext;

  public TestCustomer createCustomer(String customerName, PersonDetails initialAdmin) {
    return new TestCustomer(testContext, testContext.customerService.createCustomer(customerName, initialAdmin), initialAdmin);
  }

  public TestCustomer createCustomer() {
    return testContext.userNameSupplier.doWithUser(REALGUARDIO_ADMIN_USER,
        () -> {
          String customerName = uniqueCustomerName();
          PersonDetails initialAdmin = uniquify(INITIAL_ADMIN);

          CustomerAndCustomerEmployee createdCustomer = testContext.customerService.createCustomer(customerName, initialAdmin);
          return new TestCustomer(testContext, createdCustomer, initialAdmin);
        });
  }
}
