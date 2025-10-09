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
    osoService.createRole("CustomerEmployee", "alice", "DISARM", "Customer", "acme");
    osoService.createRelation("Location", "loc1", "customer", "Customer", "acme");
    osoService.createRelation("SecuritySystem", "ss1", "location", "Location", "loc1");

    assertIsAuthorized("alice", "disarm", "ss1");
    assertIsNotAuthorized("alice", "disarm", "ss2");
  }

  @Test
  public void shouldAuthorizeBobForCustomerFoo() {
    osoService.createRole("CustomerEmployee", "bob", "DISARM", "Customer", "foo");
    osoService.createRelation("Location", "loc2", "customer", "Customer", "foo");
    osoService.createRelation("SecuritySystem", "ss2", "location", "Location", "loc2");

    assertIsAuthorized("bob", "disarm", "ss2");
    assertIsNotAuthorized("bob", "disarm", "ss1");
  }

  @Test
  public void shouldAuthorizeMaryForLocation() {
    osoService.createRole("CustomerEmployee", "mary", "DISARM", "Location", "loc3");
    osoService.createRelation("SecuritySystem", "ss3", "location", "Location", "loc3");

    assertIsAuthorized("mary", "disarm", "ss3");
  }

  @Test
  public void shouldAuthorizeCharlieViaTeamMembership() {
    osoService.createRelation("Team", "ops-t1", "members", "CustomerEmployee", "charlie");
    osoService.createRole("Team", "ops-t1", "DISARM", "Location", "loc1");
    osoService.createRelation("SecuritySystem", "ss1", "location", "Location", "loc1");

    assertIsAuthorized("charlie", "disarm", "ss1");
    assertIsNotAuthorized("charlie", "disarm", "ss2");
    assertIsNotAuthorized("charlie", "disarm", "ss3");
  }

  private void assertIsAuthorized(String user, String action, String securitySystem) {
    assertThat(osoService.authorize("CustomerEmployee", user, action, "SecuritySystem", securitySystem)).isTrue();
  }

  private void assertIsNotAuthorized(String user, String action, String securitySystem) {
    assertThat(osoService.authorize("CustomerEmployee", user, action, "SecuritySystem", securitySystem)).isFalse();
  }

}
