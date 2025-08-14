package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;

public record CreateCustomerRequest(String name, PersonDetails initialAdministrator) {
}
