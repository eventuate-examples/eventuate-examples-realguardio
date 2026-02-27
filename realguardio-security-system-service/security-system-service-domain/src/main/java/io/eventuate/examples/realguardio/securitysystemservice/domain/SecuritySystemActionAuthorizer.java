package io.eventuate.examples.realguardio.securitysystemservice.domain;

public interface SecuritySystemActionAuthorizer {
  void isAllowed(String permission, long securitySystemId);
}
