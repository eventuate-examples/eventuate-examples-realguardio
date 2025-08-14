package io.eventuate.examples.realguardio.customerservice.customermanagement.common;

import io.eventuate.examples.realguardio.customerservice.customermanagement.common.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.customermanagement.common.PersonDetails;

public class Uniquifier {
    
    public static String uniquify(String text) {
        return text + System.currentTimeMillis();
    }
    
    public static EmailAddress uniquify(EmailAddress emailAddress) {
        String email = emailAddress.email();
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        return new EmailAddress(localPart + System.currentTimeMillis() + domain);
    }
    
    public static PersonDetails uniquify(PersonDetails personDetails) {
        return new PersonDetails(
            personDetails.name(),
            uniquify(personDetails.emailAddress())
        );
    }
}