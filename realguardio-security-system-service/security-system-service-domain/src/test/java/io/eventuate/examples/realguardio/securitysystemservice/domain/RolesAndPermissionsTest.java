package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.eventuate.examples.realguardio.securitysystemservice.domain.RolesAndPermissions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RolesAndPermissionsTest {

    @Test
    public void testPermissionsToRolesMapping() {
        assertThat(permissionsToRoles.get(ARM))
                .isEqualTo(Set.of(SECURITY_SYSTEM_ARMER));

        assertThat(permissionsToRoles.get(DISARM))
                .isEqualTo(Set.of(SECURITY_SYSTEM_DISARMER));

        assertThat(permissionsToRoles.get(VIEW))
                .isEqualTo(Set.of(SECURITY_SYSTEM_ARMER, SECURITY_SYSTEM_DISARMER, SECURITY_SYSTEM_VIEWER));
    }
}
