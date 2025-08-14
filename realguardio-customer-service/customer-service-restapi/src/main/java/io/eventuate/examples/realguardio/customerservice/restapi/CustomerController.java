package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.Customers;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerAndCustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @GetMapping
  public Customers getCustomers() {
    return new Customers(customerService.findAll());
  }

  @PostMapping
  @PreAuthorize("hasRole('REALGUARDIO_ADMIN')")
  public CustomerAndCustomerEmployee createCustomer(@RequestBody CreateCustomerRequest request) {
    return customerService.createCustomer(request.name(), request.initialAdministrator());
  }
}
