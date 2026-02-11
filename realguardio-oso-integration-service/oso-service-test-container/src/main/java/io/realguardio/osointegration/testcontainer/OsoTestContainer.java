package io.realguardio.osointegration.testcontainer;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class OsoTestContainer extends GenericContainer<OsoTestContainer> {

  private static final String OSO_IMAGE = "public.ecr.aws/osohq/dev-server:latest";
  private static final int OSO_PORT = 8080;
  private static final String OSO_TEST_TOKEN = "e_0123456789_12345_osotesttoken01xiIn";
  private static final String POLICY_FILE_CLASSPATH = "/policies/main.polar";

  public OsoTestContainer() {
    super(OSO_IMAGE);
    withExposedPorts(OSO_PORT)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource(POLICY_FILE_CLASSPATH),
                    "/policies/main.polar"
            )
            .withCommand("--watch-for-changes", "/policies/main.polar")
            .waitingFor(Wait.forListeningPort());
  }

  public void addProperties(DynamicPropertyRegistry registry) {
    registry.add("oso.url", () -> "http://localhost:" + getMappedPort(OSO_PORT));
    registry.add("oso.auth", () -> OSO_TEST_TOKEN);
  }
}
