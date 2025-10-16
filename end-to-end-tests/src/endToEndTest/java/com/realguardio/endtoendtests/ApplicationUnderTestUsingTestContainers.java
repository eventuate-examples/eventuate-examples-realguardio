package com.realguardio.endtoendtests;

import io.eventuate.cdc.testcontainers.EventuateCdcContainer;
import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.BuildArgsResolver;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.lifecycle.Startables;

import java.nio.file.Paths;

public class ApplicationUnderTestUsingTestContainers implements ApplicationUnderTest {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationUnderTestUsingTestContainers.class);

  public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("e2e-tests");

  public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
      .withNetworkAliases("kafka")
      .withReuse(true)
      ;

  public static EventuateDatabaseContainer<?> customerDatabase = DatabaseContainerFactory.makeVanillaDatabaseContainer()
      .withNetwork(eventuateKafkaCluster.network)
      .withNetworkAliases("customer-service-db")
      .withReuse(true);
  public static EventuateDatabaseContainer<?> securityDatabase = DatabaseContainerFactory.makeVanillaDatabaseContainer()
      .withNetwork(eventuateKafkaCluster.network)
      .withNetworkAliases("security-system-service-db")
      .withReuse(true);
  public static EventuateDatabaseContainer<?> orchestrationDatabase = DatabaseContainerFactory.makeVanillaDatabaseContainer()
      .withNetwork(eventuateKafkaCluster.network)
      .withNetworkAliases("orchestration-service-db")
      .withReuse(true);

  public static AuthorizationServerContainerForServiceContainers iamService = new AuthorizationServerContainerForServiceContainers()
      .withUserDb()
      .withNetwork(eventuateKafkaCluster.network)
      .withNetworkAliases("iam-service")
      .withReuse(true);

  public static OsoTestContainer osoService = new OsoTestContainer()
      .withNetwork(eventuateKafkaCluster.network)
      .withNetworkAliases("oso-service")
      .withReuse(true)
      .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-service:"));

  public static GenericContainer<?> customerService =
      ServiceContainer.makeFromDockerfileInFileSystem("../realguardio-customer-service/Dockerfile-local")
          .withNetwork(eventuateKafkaCluster.network)
          .withNetworkAliases("customer-service")
          .withDatabase(customerDatabase)
          .withKafka(kafka)
          .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
          .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
          .withEnv("SPRING_PROFILES_ACTIVE", "docker")
          .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
          .withReuse(true)
          .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC customer-service:"))
      ;

  public static GenericContainer<?> orchestrationService =
      ServiceContainer.makeFromDockerfileInFileSystem("../realguardio-orchestration-service/Dockerfile-local")
          .withNetwork(eventuateKafkaCluster.network)
          .withNetworkAliases("orchestration-service")
          .withDatabase(orchestrationDatabase)
          .withKafka(kafka)
          .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
          .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
          .withEnv("SPRING_PROFILES_ACTIVE", "docker")
          .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
          .withReuse(true)
          .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC orchestration-service:"));

  public static final GenericContainer<?> securitySystemService =
      new ServiceContainer(new ImageFromDockerfile()
          .withFileFromPath(".", Paths.get("..").toAbsolutePath())  // Context: parent directory
          .withDockerfilePath("realguardio-security-system-service/Dockerfile-local")  // Dockerfile path
          .withBuildArgs(BuildArgsResolver.buildArgs()))

          .withNetwork(eventuateKafkaCluster.network)
          .withNetworkAliases("security-system-service")
          .withDatabase(securityDatabase)
          .withKafka(kafka)
          .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
          .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
          .withEnv("SPRING_PROFILES_ACTIVE", "docker")
          .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
          .withEnv("CUSTOMER_SERVICE_URL", "http://customer-service:8080")
          .withEnv("OSO_URL", "http://oso-service:8080")
          .withEnv("OSO_AUTH", "e_0123456789_12345_osotesttoken01xiIn")

          .withReuse(false)
          .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC security-service:"));

  public static GenericContainer<?> osoIntegrationService =
      ServiceContainer.makeFromDockerfileInFileSystem("../realguardio-oso-integration-service/Dockerfile-local")
          .withNetwork(eventuateKafkaCluster.network)
          .withNetworkAliases("oso-integration-service")
          .withKafka(kafka)
          .withEnv("OSO_URL", "http://oso-service:8080")
          .withEnv("OSO_AUTH", "e_0123456789_12345_osotesttoken01xiIn")
          .withEnv("SPRING_PROFILES_ACTIVE", "docker")
          .withReuse(false)
          .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-integration-service:"));

  private static final EventuateCdcContainer cdc = new EventuateCdcContainer()
      .withKafka(kafka)
      .withKafkaLeadership()
      .withTramPipeline(customerDatabase)
      .withTramPipeline(securityDatabase)
      .withTramPipeline(orchestrationDatabase)
      .withReuse(false)
      .withExposedPorts(8080)
      .dependsOn(customerService, securitySystemService, orchestrationService, osoIntegrationService)
      .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC cdc:"));


  @Override
  public void start() {
    kafka.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("kafka:"));
    Startables.deepStart(
        kafka,
        iamService,
        osoService,
        customerService,
        orchestrationService,
        securitySystemService,
        osoIntegrationService,
        cdc
    ).join();

  }

  @Override
  public void useLocationRolesReplica() {
    securitySystemService.withEnv("SPRING_PROFILES_ACTIVE", "UseRolesReplica");
  }

  @Override
  public void useOsoService() {
    securitySystemService.withEnv("SPRING_PROFILES_ACTIVE", "UseOsoService");
  }

  @Override
  public int getCustomerServicePort() {
    return customerService.getFirstMappedPort();
  }

  @Override
  public int getOrchestrationServicePort() {
    return orchestrationService.getFirstMappedPort();
  }

  @Override
  public int getSecurityServicePort() {
    return securitySystemService.getFirstMappedPort();
  }

  @Override
  public int getIamPort() {
    return iamService.getFirstMappedPort();
  }

  @Override
  public String iamServiceHostAndPort() {
    return "iam-service:9000";
  }
}
