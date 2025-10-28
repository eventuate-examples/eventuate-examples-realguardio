package io.eventuate.examples.realguardio.securitysystemservice.domain;

public interface SecuritySystemActionAuthorizer {
  void verifyCanArm(long securitySystemId);
  void verifyCanDisarm(long securitySystemId);
  void verifyCanView(long securitySystemId);
}
