package io.eventuate.examples.realguardio.customerservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Paths;
import java.time.Duration;

public class CustomerServiceContainer extends GenericContainer<CustomerServiceContainer> {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceContainer.class);
    private static final int CONTAINER_PORT = 3001;
    private final PostgreSQLContainer<?> postgres;
    private final GenericContainer<?> iamService;
    
    public CustomerServiceContainer(PostgreSQLContainer<?> postgres, GenericContainer<?> iamService) {
        super(new ImageFromDockerfile()
                .withDockerfile(Paths.get("../Dockerfile-local"))
                .withBuildArg("baseImageVersion", "0.1.0.RELEASE")
                .withBuildArg("serviceImageVersion", System.getProperty("project.version", "0.1.0-SNAPSHOT")));
        this.postgres = postgres;
        this.iamService = iamService;
        configureContainer();

    }
    
    private void configureContainer() {
        Network network = Network.newNetwork();
        
        postgres.withNetwork(network)
                .withNetworkAliases("postgres")
                .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("POSTGRES"));
        
        iamService.withNetwork(network)
                .withNetworkAliases("iam-service")
                .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("IAM"));
        
        this.withNetwork(network)
            .withExposedPorts(CONTAINER_PORT)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/test")
            .withEnv("SPRING_DATASOURCE_USERNAME", postgres.getUsername())
            .withEnv("SPRING_DATASOURCE_PASSWORD", postgres.getPassword())
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop")
            .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
            .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
            .withEnv("SERVER_PORT", String.valueOf(CONTAINER_PORT))
            .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SERVICE"))
            .waitingFor(Wait.forHttp("/actuator/health")
                .forPort(CONTAINER_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(60)))
            .dependsOn(postgres, iamService);
    }

    public Integer getMappedServicePort() {
        return getMappedPort(CONTAINER_PORT);
    }
    
    public String getBaseUrl() {
        return String.format("http://localhost:%d", getMappedPort(CONTAINER_PORT));
    }
    
    public WebClient createWebClient() {
        return WebClient.builder()
                .baseUrl(getBaseUrl())
                .build();
    }
}