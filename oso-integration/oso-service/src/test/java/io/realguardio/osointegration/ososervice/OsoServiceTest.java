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
  public void shouldCreateRoleForCustomerEmployeeAtCustomer() {
    osoService.createRole("CustomerEmployee", "alice", "DISARM", "Customer", "acme");
    osoService.createRelation("Location", "loc1", "customer", "Customer", "acme");
    osoService.createRelation("SecuritySystem", "ss1", "location", "Location", "loc1");

    assertThat(osoService.authorize("CustomerEmployee", "alice", "disarm", "SecuritySystem", "ss1")).isTrue();
    assertThat(osoService.authorize("CustomerEmployee", "alice", "disarm", "SecuritySystem", "ss2")).isFalse();
  }
}
