package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public interface CustomerRepositoryCustom {
  Customer findRequiredById(long customerId);
}