package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Set;

public interface UserNameSupplier {
    String getCurrentUserName();
    Set<String> getCurrentUserRoles();
    boolean isCustomerEmployee();
}