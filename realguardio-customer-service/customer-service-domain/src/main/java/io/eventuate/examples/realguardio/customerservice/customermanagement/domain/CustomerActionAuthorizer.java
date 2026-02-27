package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public interface CustomerActionAuthorizer {
  void isAllowed(String permission, long customerId);
}
