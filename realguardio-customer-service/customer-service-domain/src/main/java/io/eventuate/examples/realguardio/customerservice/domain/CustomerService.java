package io.eventuate.examples.realguardio.customerservice.domain;

import java.util.List;

public interface CustomerService {
    List<Customer> findAll();
}