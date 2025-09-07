package com.realguardio.endtoendtests;

import org.junit.jupiter.api.BeforeAll;

public class RealGuardioEndToEndTest extends AbstractRealGuardioEndToEndTest{

    @BeforeAll
    static void startContainers() {
        configureRestAssured();
        aut.start();
    }

    @Override
    protected void waitForUntilPermissionsHaveBeenAssigned(String adminAuthToken, String adminEmail, Long locationId) {
    }
}