package com.realguardio.endtoendtests;

import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

/**
 * End-to-end test for local authorization with the OsoLocalSecuritySystemLocation profile.
 *
 * This test verifies:
 * 1. Security systems are created with correct locationId
 * 2. Authorization works correctly using local data bindings for SecuritySystem-Location
 * 3. No SecuritySystemAssignedToLocation events need to be published to Oso Cloud
 */
public class RealGuardioLocalAuthorizationEndToEndTest extends AbstractRealGuardioEndToEndTest {

    @BeforeAll
    static void startContainers() {
        aut = makeAut("e2e-local-auth");
        configureRestAssured();
        aut.useOsoLocalSecuritySystemLocation();
        aut.start();
    }

    @Override
    protected void waitForUntilPermissionsHaveBeenAssigned(String adminAuthToken, String adminEmail, Long locationId) {
        // With local authorization for SecuritySystem-Location, we still need to wait for
        // user role facts to propagate to Oso Cloud (Location-User role assignments)
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void waitForCustomerAdminPermission(String adminEmail, long customerId) {
        // Wait for Oso permission propagation for customer admin role
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
