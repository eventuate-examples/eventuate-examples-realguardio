package io.eventuate.examples.realguardio.customerservice.restapi;

import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;

public record CreateEmployeeRequest(PersonDetails personDetails) {
}