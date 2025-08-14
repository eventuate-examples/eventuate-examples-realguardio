package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public interface TeamRepositoryCustom {
  Team findRequiredById(long teamId);
}