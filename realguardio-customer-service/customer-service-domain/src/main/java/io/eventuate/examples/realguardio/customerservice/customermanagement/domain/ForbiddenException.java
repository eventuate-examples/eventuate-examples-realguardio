package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
