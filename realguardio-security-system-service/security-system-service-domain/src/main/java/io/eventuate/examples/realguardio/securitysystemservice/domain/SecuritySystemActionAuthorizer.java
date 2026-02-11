package io.eventuate.examples.realguardio.securitysystemservice.domain;

public interface SecuritySystemActionAuthorizer {
  void verifyCanDo(long securitySystemId, String permission);
}
