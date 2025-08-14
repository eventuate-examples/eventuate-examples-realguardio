package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public interface CustomerEmployeeRepositoryCustom {
  CustomerEmployee findRequiredById(long customerEmployeeId);
}