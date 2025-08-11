package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerService;
import io.eventuate.examples.realguardio.customerservice.domain.Customers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping("/securitysystems")
  public Customers getCustomers() {
    return new Customers(customerService.findAll());
  }
}
