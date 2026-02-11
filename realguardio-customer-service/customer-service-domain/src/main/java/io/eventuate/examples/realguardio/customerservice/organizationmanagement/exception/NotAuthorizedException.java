package io.eventuate.examples.realguardio.customerservice.organizationmanagement.exception;

/**
 * Exception thrown when a user is not authorized to perform an action.
 */
public class NotAuthorizedException extends RuntimeException {

    public NotAuthorizedException(String message) {
        super(message);
    }

    public NotAuthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}