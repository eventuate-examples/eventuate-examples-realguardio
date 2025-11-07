package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public interface CustomerActionAuthorizer {
  void verifyCanDo(long customerId, String permission);
}
