package io.realguardio.osointegration.ososervice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class OsoServiceTest {
  @Container
  static GenericContainer<?> osoDevServer = new GenericContainer<>("public.ecr.aws/osohq/dev-server:latest")
          .withExposedPorts(8080)
          .withCopyFileToContainer(
                  MountableFile.forHostPath("../policies/main.polar"),
                  "/policies/main.polar"
          )
          .withCommand("--watch-for-changes", "/policies/main.polar")
          .waitingFor(Wait.forListeningPort());

  @DynamicPropertySource
  static void setOsoProperties(DynamicPropertyRegistry registry) {
    registry.add("oso.url", () -> "http://localhost:" + osoDevServer.getMappedPort(8080));
    registry.add("oso.auth", () -> "e_0123456789_12345_osotesttoken01xiIn");
  }

  @Autowired
  private OsoService osoService;


  @Test
  public void shouldAuthorizeBobForCustomerAcme() {
    createRoleInCustomer("alice", "acme");
    createLocationForCustomer("loc1", "acme");
    assignSecuritySystemToLocation("ss1", "loc1");

    assertIsAuthorized("alice", "disarm", "ss1");
    assertIsNotAuthorized("alice", "disarm", "ss2");
  }


  @Test
  public void shouldAuthorizeBobForCustomerFoo() {
    createRoleInCustomer("bob", "foo");
    createLocationForCustomer("loc2", "foo");
    assignSecuritySystemToLocation("ss2", "loc2");

    assertIsAuthorized("bob", "disarm", "ss2");
    assertIsNotAuthorized("bob", "disarm", "ss1");
  }

  @Test
  public void shouldAuthorizeMaryForLocation() {
    createRoleAtLocation("mary", "loc3");
    assignSecuritySystemToLocation("ss3", "loc3");

    assertIsAuthorized("mary", "disarm", "ss3");
  }

  @Test
  public void shouldAuthorizeCharlieViaTeamMembership() {
    addToTeam("charlie", "ops-t1");
    createTeamRoleAtLocation("ops-t1", "loc1");
    assignSecuritySystemToLocation("ss1", "loc1");

    assertIsAuthorized("charlie", "disarm", "ss1");
    assertIsNotAuthorized("charlie", "disarm", "ss2");
    assertIsNotAuthorized("charlie", "disarm", "ss3");
  }

  private void createTeamRoleAtLocation(String team, String location) {
    osoService.createRole("Team", team, "DISARM", "Location", location);
  }

  private void createLocationForCustomer(String location, String customer) {
    osoService.createRelation("Location", location, "customer", "Customer", customer);
  }

  private void assignSecuritySystemToLocation(String securitySystem, String location) {
    osoService.createRelation("SecuritySystem", securitySystem, "location", "Location", location);
  }

  private void addToTeam(String user, String team) {
    osoService.createRelation("Team", team, "members", "CustomerEmployee", user);
  }

  private void createRoleInCustomer(String user, String company) {
    osoService.createRole("CustomerEmployee", user, "DISARM", "Customer", company);
  }

  private void createRoleAtLocation(String user, String location) {
    osoService.createRole("CustomerEmployee", user, "DISARM", "Location", location);
  }

  private void assertIsAuthorized(String user, String action, String securitySystem) {
    assertThat(osoService.authorize("CustomerEmployee", user, action, "SecuritySystem", securitySystem)).isTrue();
  }

  private void assertIsNotAuthorized(String user, String action, String securitySystem) {
    assertThat(osoService.authorize("CustomerEmployee", user, action, "SecuritySystem", securitySystem)).isFalse();
  }

}
