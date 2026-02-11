package com.realguardio.endtoendtests.dto;

public record CreateCustomerRequest(String name, PersonDetails initialAdministrator) {
}