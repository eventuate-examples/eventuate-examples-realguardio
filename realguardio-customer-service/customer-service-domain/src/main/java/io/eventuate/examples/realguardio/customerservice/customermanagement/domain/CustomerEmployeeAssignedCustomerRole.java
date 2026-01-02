package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record CustomerEmployeeAssignedCustomerRole(Long customerEmployeeId, String userName, String roleName) implements CustomerEvent {
}
