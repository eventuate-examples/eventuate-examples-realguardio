package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.exception.NotAuthorizedException;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@Profile("!UseOsoService")
public class LocalCustomerActionAuthorizer implements CustomerActionAuthorizer {

    private static final Logger logger = LoggerFactory.getLogger(LocalCustomerActionAuthorizer.class);

    private final UserNameSupplier userNameSupplier;
    private final CustomerEmployeeRepository customerEmployeeRepository;

    public LocalCustomerActionAuthorizer(UserNameSupplier userNameSupplier, CustomerEmployeeRepository customerEmployeeRepository) {
        this.userNameSupplier = userNameSupplier;
        this.customerEmployeeRepository = customerEmployeeRepository;
    }

    @Override
    public void isAllowed(String permission, long customerId) {
        Set<String> requiredRoles = RolesAndPermissions.rolesForPermission(permission);
        if (requiredRoles == null)
            throw new NotAuthorizedException("Don't recognize permission: %s".formatted(permission));
        verifyCustomerEmployeeHasRequiredRolesInCustomer(customerId, requiredRoles);
    }


    private void verifyCustomerEmployeeHasRequiredRolesInCustomer(Long customerId, Set<String> requiredRoles) {
        String userId = userNameSupplier.getCurrentUserEmail();
        Set<String> currentUserRolesAtCustomer = customerEmployeeRepository.findRolesInCustomer(customerId, userId);
        if (Collections.disjoint(currentUserRolesAtCustomer, requiredRoles)) {
            logger.warn("User {} lacks {} permission for customerId {}. Only has {}", userId, requiredRoles, customerId, currentUserRolesAtCustomer);
            throw new NotAuthorizedException(
                    String.format("User %s lacks %s permission for customerId %d. Only has %s",
                            userId, requiredRoles, customerId, currentUserRolesAtCustomer)
            );
        }
    }

}
