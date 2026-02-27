package io.eventuate.examples.realguardio.securitysystemservice.domain;

public abstract class AbstractSecuritySystemActionAuthorizer  implements SecuritySystemActionAuthorizer {
    protected final UserNameSupplier userNameSupplier;

    public AbstractSecuritySystemActionAuthorizer(UserNameSupplier userNameSupplier) {
        this.userNameSupplier = userNameSupplier;
    }

    public void isAllowed(String permission, long securitySystemId) {
        if (userNameSupplier.isCustomerEmployee())
            isAllowedForCustomerEmployee(permission, securitySystemId);
    }

    protected abstract void isAllowedForCustomerEmployee(String permission, long securitySystemId);
}
