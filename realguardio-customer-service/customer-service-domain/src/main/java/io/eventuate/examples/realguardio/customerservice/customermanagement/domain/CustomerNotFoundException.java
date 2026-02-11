package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}