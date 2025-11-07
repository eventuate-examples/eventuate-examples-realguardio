package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.organizationmanagement.exception.NotAuthorizedException;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalCustomerActionAuthorizerTest {

    private LocalCustomerActionAuthorizer authorizer;

    @Mock
    private UserNameSupplier userNameSupplier;

    @Mock
    private CustomerEmployeeRepository customerEmployeeRepository;

    private static final Long CUSTOMER_ID = 123L;
    private static final String USER_EMAIL = "user@example.com";

    @BeforeEach
    public void setUp() {
        authorizer = new LocalCustomerActionAuthorizer(userNameSupplier, customerEmployeeRepository);
    }

    @Test
    void shouldAllowActionWhenUserHasRequiredRole() {
        when(userNameSupplier.getCurrentUserEmail()).thenReturn(USER_EMAIL);
        when(customerEmployeeRepository.findRolesInCustomer(CUSTOMER_ID, USER_EMAIL))
                .thenReturn(Set.of(RolesAndPermissions.COMPANY_ROLE_ADMIN));

        authorizer.verifyCanDo(CUSTOMER_ID, RolesAndPermissions.CREATE_CUSTOMER_EMPLOYEE);
    }

    @Test
    void shouldThrowNotAuthorizedExceptionWhenUserLacksRequiredRole() {
        when(userNameSupplier.getCurrentUserEmail()).thenReturn(USER_EMAIL);
        when(customerEmployeeRepository.findRolesInCustomer(CUSTOMER_ID, USER_EMAIL))
                .thenReturn(Set.of("SOME_OTHER_ROLE"));

        assertThatThrownBy(() -> authorizer.verifyCanDo(CUSTOMER_ID, RolesAndPermissions.CREATE_CUSTOMER_EMPLOYEE))
                .isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    void shouldThrowNotAuthorizedExceptionWhenUserHasNoRoles() {
        when(userNameSupplier.getCurrentUserEmail()).thenReturn(USER_EMAIL);
        when(customerEmployeeRepository.findRolesInCustomer(CUSTOMER_ID, USER_EMAIL))
                .thenReturn(Set.of());

        assertThatThrownBy(() -> authorizer.verifyCanDo(CUSTOMER_ID, RolesAndPermissions.CREATE_CUSTOMER_EMPLOYEE))
                .isInstanceOf(NotAuthorizedException.class);
    }

    @Test
    void shouldThrowNotAuthorizedExceptionWhenPermissionNotRecognized() {
        assertThatThrownBy(() -> authorizer.verifyCanDo(CUSTOMER_ID, "unknownPermission"))
                .isInstanceOf(NotAuthorizedException.class);

        verifyNoInteractions(userNameSupplier);
        verifyNoInteractions(customerEmployeeRepository);
    }
}