package com.realguardio.endtoendtests;

import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

public class RealGuardioEndToEndUsingOsoService extends AbstractRealGuardioEndToEndTest{

    @BeforeAll
    static void startContainers() {
        aut.useOsoService();
        configureRestAssured();
        aut.start();
    }

    @Override
    protected void waitForUntilPermissionsHaveBeenAssigned(String adminAuthToken, String adminEmail, Long locationId) {
      try {
        TimeUnit.SECONDS.sleep(10);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
}