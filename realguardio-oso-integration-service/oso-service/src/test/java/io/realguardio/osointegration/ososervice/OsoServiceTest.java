package io.realguardio.osointegration.ososervice;

import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest()
@Testcontainers
public class OsoServiceTest {

  @Configuration
  @EnableAutoConfiguration
  @ComponentScan
  static class Config {
  }

  @Container
  static OsoTestContainer osoDevServer = new OsoTestContainer();

  @DynamicPropertySource
  static void setOsoProperties(DynamicPropertyRegistry registry) {
    osoDevServer.addProperties(registry);
  }

  @Autowired
  private OsoService osoService;

  @Autowired
  private RealGuardOsoFactManager realGuardOsoFactManager;

  @Autowired
  private RealGuardOsoAuthorizer realGuardOsoAuthorizer;

  @Test
  public void shouldAuthorizeAliceForCustomerAcme() {
    realGuardOsoFactManager.createRoleInCustomer("alice", "acme", "SECURITY_SYSTEM_DISARMER");
    realGuardOsoFactManager.createLocationForCustomer("loc1", "acme");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss1", "loc1");

    assertIsAuthorized("alice", "disarm", "ss1");
    assertIsNotAuthorized("alice", "disarm", "ss2");
    assertIsNotAuthorized("alice", "arm", "ss1");

    realGuardOsoFactManager.createRoleInCustomer("alice", "acme", "SECURITY_SYSTEM_ARMER");
    assertIsAuthorized("alice", "arm", "ss1");
  }


  @Test
  public void shouldAuthorizeBobForCustomerFoo() {
    realGuardOsoFactManager.createRoleInCustomer("bob", "foo", "SECURITY_SYSTEM_DISARMER");
    realGuardOsoFactManager.createLocationForCustomer("loc2", "foo");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss2", "loc2");

    assertIsAuthorized("bob", "disarm", "ss2");
    assertIsNotAuthorized("bob", "disarm", "ss1");
  }

  @Test
  public void shouldAuthorizeMaryForLocation() {
    realGuardOsoFactManager.createRoleAtLocation("mary", "loc3", "SECURITY_SYSTEM_DISARMER");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss3", "loc3");

    assertIsAuthorized("mary", "disarm", "ss3");
    assertIsNotAuthorized("mary", "arm", "ss3");

    realGuardOsoFactManager.createRoleAtLocation("mary", "loc3", "SECURITY_SYSTEM_ARMER");
    assertIsAuthorized("mary", "arm", "ss3");
  }

  @Test
  public void shouldAuthorizeCharlieViaTeamMembership() {
    realGuardOsoFactManager.addToTeam("charlie", "ops-t1");
    realGuardOsoFactManager.createTeamRoleAtLocation("ops-t1", "loc1", "SECURITY_SYSTEM_DISARMER");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss1", "loc1");

    assertIsAuthorized("charlie", "disarm", "ss1");
    assertIsNotAuthorized("charlie", "disarm", "ss2");
    assertIsNotAuthorized("charlie", "disarm", "ss3");

    assertIsNotAuthorized("charlie", "arm", "ss1");
    realGuardOsoFactManager.createTeamRoleAtLocation("ops-t1", "loc1", "SECURITY_SYSTEM_ARMER");
    assertIsAuthorized("charlie", "arm", "ss1");
  }


  private void assertIsAuthorized(String user, String action, String securitySystem) {
      Boolean result;
      try {
          result = realGuardOsoAuthorizer.isAuthorized(user, action, securitySystem).get();
      } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
      }
      assertThat(result).isTrue();
  }

  private void assertIsNotAuthorized(String user, String action, String securitySystem) {
      Boolean result;
      try {
          result = realGuardOsoAuthorizer.isAuthorized(user, action, securitySystem).get();
      } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
      }
      assertThat(result).isFalse();
  }

}
