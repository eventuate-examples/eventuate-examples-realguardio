package com.realguardio.endtoendtests;

import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

public class RealGuardioEndToEndTest extends AbstractRealGuardioEndToEndTest{

    @BeforeAll
    static void startContainers() {
        configureRestAssured();
        aut.start();
    }

    @Override
    protected void waitForUntilPermissionsHaveBeenAssigned(String adminAuthToken, String adminEmail, Long locationId) {
    }

    @Override
    protected void waitForCustomerAdminPermission(String adminEmail, long customerId) {
        // Wait for Oso permission propagation since docker-compose uses UseOsoService profile
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}