package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonDetails;
import io.eventuate.examples.realguardio.customerservice.commondomain.PersonName;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport.MockUser;
import io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier;


public class CustomerServiceTestData {

    // Application roles

    public static final String ROLE_ADMIN = "REALGUARDIO_ADMIN";
    public static final MockUser REALGUARDIO_ADMIN_USER = new MockUser("admin@realguard.io", ROLE_ADMIN);

    // Customer roles

    public static final String ROLE_CUSTOMER_EMPLOYEE = "REALGUARDIO_CUSTOMER_EMPLOYEE";
    public static final String ROLE_CUSTOMER_ADMIN = "REALGUARDIO_CUSTOMER_ADMIN";

    // Users
    public static final MockUser EMPLOYEE_USER = new MockUser("employee@realguard.io", ROLE_CUSTOMER_EMPLOYEE);
    
    // Person Details
    public static final PersonDetails ADMIN_PERSON = new PersonDetails(
        new PersonName("Admin", "User"),
        new EmailAddress("admin@realguard.io"));
        
    public static final PersonDetails JOHN_DOE = new PersonDetails(
        new PersonName("John", "Doe"),
        new EmailAddress("john@realguard.io"));
        
    public static final PersonDetails NON_ADMIN = new PersonDetails(
        new PersonName("NonAdmin", "User"),
        new EmailAddress("nonadmin@realguard.io"));
        
    public static final PersonDetails NEW_EMPLOYEE = new PersonDetails(
        new PersonName("New", "Employee"),
        new EmailAddress("newemployee@realguard.io"));
        
    public static final PersonDetails JOHN_DOE_DETAILS = new PersonDetails(
        new PersonName("John", "Doe"),
        new EmailAddress("john.doe@realguard.io"));
        
    public static final PersonDetails MARY_SMITH = new PersonDetails(
        new PersonName("Mary", "Smith"),
        new EmailAddress("mary.smith@realguard.io"));
        
    public static final PersonDetails INITIAL_ADMIN = new PersonDetails(
        new PersonName("Initial", "Admin"),
        new EmailAddress("initial.admin@realguard.io"));
    public static final MockUser INITIAL_ADMIN_USER = new MockUser("initial.admin@realguard.io", ROLE_CUSTOMER_EMPLOYEE);

    // Customer names
    public static final String TEST_CUSTOMER_NAME = "Test Co";
    public static final String TEST_COMPANY_NAME = "Test Company";
    public static final String ALARM_CUSTOMER_NAME = "Alarm Customer Inc";
    
    // Locations
    public static final String MAIN_OFFICE_LOCATION = "Main Office";
    
    // Roles
    public static final String SECURITY_SYSTEM_DISARMER_ROLE = "SecuritySystemDisarmer";
    public static final String SECURITY_SYSTEM_ARMER_ROLE = "SecuritySystemArmer";
    
    // Test IDs
    public static final Long NONEXISTENT_CUSTOMER_ID = 1L;

    public static String uniqueCustomerName() {
      return Uniquifier.uniquify(ALARM_CUSTOMER_NAME);
    }
}