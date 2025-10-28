package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalSecuritySystemActionAuthorizerTest {

  @Mock
  private SecuritySystemRepository securitySystemRepository;

  @Mock
  private CustomerServiceClient customerServiceClient;

  @Mock
  private UserNameSupplier userNameSupplier;
  private LocalSecuritySystemActionAuthorizer localSecuritySystemActionAuthorizer;

  Long systemId = 1L;
  Long locationId = 456L;
  String userId = "employee@example.com";

  @BeforeEach
  void setUp() throws Exception {
    localSecuritySystemActionAuthorizer = new LocalSecuritySystemActionAuthorizer(customerServiceClient, securitySystemRepository, userNameSupplier);

    // Given
    // new HashSet<>(Arrays.asList(SecuritySystemAction.DISARM))
    SecuritySystem securitySystem = new SecuritySystem("Office Front Door", SecuritySystemState.ARMED);
    setId(securitySystem, systemId);
    securitySystem.setLocationId(locationId);

    when(userNameSupplier.getCurrentUserName()).thenReturn(userId);
    when(securitySystemRepository.findById(systemId)).thenReturn(Optional.of(securitySystem));
  }


  @Test
  void employeeWithDisarmRoleIsAllowed() throws Exception {

    when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
        .thenReturn(Set.of(RolesAndPermissions.SECURITY_SYSTEM_DISARMER, "VIEW_ALERTS"));

    localSecuritySystemActionAuthorizer.verifyCanDo(systemId, RolesAndPermissions.DISARM);
  }

  @Test
  void employeeWithoutDisarmRoleIsNotAllowed() {

    when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
        .thenReturn(Set.of("VIEW_ALERTS"));

    assertThatThrownBy(() -> localSecuritySystemActionAuthorizer.verifyCanDo(systemId, RolesAndPermissions.DISARM))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void employeeWithArmRoleIsAllowed() throws Exception {

    when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
        .thenReturn(Set.of(RolesAndPermissions.SECURITY_SYSTEM_ARMER, "VIEW_ALERTS"));

    localSecuritySystemActionAuthorizer.verifyCanDo(systemId, RolesAndPermissions.ARM);
  }

  @Test
  void employeeWithoutArmRoleIsNotAllowed() {

    when(customerServiceClient.getUserRolesAtLocation(userId, locationId))
        .thenReturn(Set.of("VIEW_ALERTS"));

    assertThatThrownBy(() -> localSecuritySystemActionAuthorizer.verifyCanDo(systemId, RolesAndPermissions.ARM))
        .isInstanceOf(ForbiddenException.class);
  }

  private static void setId(SecuritySystem system, Long id) throws Exception {
    Field idField = SecuritySystem.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(system, id);
  }

}