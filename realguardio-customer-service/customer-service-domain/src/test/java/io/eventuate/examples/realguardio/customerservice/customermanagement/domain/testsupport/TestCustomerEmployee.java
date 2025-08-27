package io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport;

import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;

public record TestCustomerEmployee(CustomerEmployee customerEmployee,
                                   PersonDetails employeeDetails) {
}
