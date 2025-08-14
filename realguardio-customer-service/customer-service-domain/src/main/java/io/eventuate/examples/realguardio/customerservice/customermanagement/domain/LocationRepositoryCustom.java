package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public interface LocationRepositoryCustom {
  Location findRequiredById(long locationId);
}