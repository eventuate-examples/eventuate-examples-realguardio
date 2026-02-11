package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record LocationCreatedForCustomer(Long locationId) implements CustomerEvent {
}
