package io.eventuate.examples.realguardio.securitysystemservice.domain;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}