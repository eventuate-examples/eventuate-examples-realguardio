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
  public void shouldAuthorizeBobForCustomerAcme() {
    realGuardOsoFactManager.createRoleInCustomer("alice", "acme", "DISARM");
    realGuardOsoFactManager.createLocationForCustomer("loc1", "acme");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss1", "loc1");

    assertIsAuthorized("alice", "disarm", "ss1");
    assertIsNotAuthorized("alice", "disarm", "ss2");
  }


  @Test
  public void shouldAuthorizeBobForCustomerFoo() {
    realGuardOsoFactManager.createRoleInCustomer("bob", "foo", "DISARM");
    realGuardOsoFactManager.createLocationForCustomer("loc2", "foo");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss2", "loc2");

    assertIsAuthorized("bob", "disarm", "ss2");
    assertIsNotAuthorized("bob", "disarm", "ss1");
  }

  @Test
  public void shouldAuthorizeMaryForLocation() {
    realGuardOsoFactManager.createRoleAtLocation("mary", "loc3", "DISARM");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss3", "loc3");

    assertIsAuthorized("mary", "disarm", "ss3");
  }

  @Test
  public void shouldAuthorizeCharlieViaTeamMembership() {
    realGuardOsoFactManager.addToTeam("charlie", "ops-t1");
    realGuardOsoFactManager.createTeamRoleAtLocation("ops-t1", "loc1", "DISARM");
    realGuardOsoFactManager.assignSecuritySystemToLocation("ss1", "loc1");

    assertIsAuthorized("charlie", "disarm", "ss1");
    assertIsNotAuthorized("charlie", "disarm", "ss2");
    assertIsNotAuthorized("charlie", "disarm", "ss3");
  }


  private void assertIsAuthorized(String user, String action, String securitySystem) {
    assertThat(realGuardOsoAuthorizer.isAuthorized(user, action, securitySystem)).isTrue();
  }

  private void assertIsNotAuthorized(String user, String action, String securitySystem) {
    assertThat(realGuardOsoAuthorizer.isAuthorized(user, action, securitySystem)).isFalse();
  }

}
