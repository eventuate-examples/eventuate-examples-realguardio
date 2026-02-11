package io.eventuate.examples.realguardio.customerservice.customermanagement;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer;

import java.util.List;

public record Customers(List<Customer> customers) {

}
