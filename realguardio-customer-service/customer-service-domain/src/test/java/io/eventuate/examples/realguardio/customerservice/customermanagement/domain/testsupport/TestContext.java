package io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestContext {

  @Autowired
  public CustomerService customerService;

  @Autowired
  public LoggedInUser userNameSupplier;

}