package io.eventuate.examples.realguardio.securitysystemservice.domain;

public interface JwtProvider {
    String getCurrentJwtToken();
}