package com.realguardio.endtoendtests.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateCustomerResponse(Customer customer, Employee initialAdministrator) {}