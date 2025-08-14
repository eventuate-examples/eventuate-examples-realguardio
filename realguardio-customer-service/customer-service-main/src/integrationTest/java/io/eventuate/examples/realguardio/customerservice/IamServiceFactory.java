package io.eventuate.examples.realguardio.customerservice;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Paths;
import java.time.Duration;

public class IamServiceFactory {
  static GenericContainer makeIamService() {
    return makeContainer()
        .withExposedPorts(9000)
        .withEnv("SPRING_PROFILES_ACTIVE", "realguardio")
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("IAM-SERVICE")))
        .waitingFor(Wait.forHttp("/actuator/health").forPort(9000).forStatusCode(200))
        .withStartupTimeout(Duration.ofSeconds(60))
        ;
  }

  private static @NotNull GenericContainer makeContainer() {
    if (false) {
      return new GenericContainer<>(DockerImageName.parse("eventuate-examples-realguardio-realguardio-iam-service:latest"));
    } else {
      return new GenericContainer<>(new ImageFromDockerfile()
          .withDockerfile(Paths.get("../../realguardio-iam-service/Dockerfile"))
          .withBuildArg("baseImageVersion", "0.1.0.RELEASE")
          .withBuildArg("serviceImageVersion", System.getProperty("project.version", "0.1.0-SNAPSHOT"))
      );
    }
  }
}
