package com.realguardio.endtoendtests;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realguardio.endtoendtests.dto.Administrator;
import com.realguardio.endtoendtests.dto.CreateCustomerRequest;
import com.realguardio.endtoendtests.dto.CreateCustomerResponse;
import com.realguardio.endtoendtests.dto.EmailAddress;
import com.realguardio.endtoendtests.dto.PersonDetails;
import com.realguardio.endtoendtests.dto.PersonName;
import com.realguardio.endtoendtests.utils.JwtTokenHelper;
import io.eventuate.cdc.testcontainers.EventuateCdcContainer;
import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RealGuardioEndToEndTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RealGuardioEndToEndTest.class);
    
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
            .withNetworkAliases("security-service-db")
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
        ServiceContainer.makeFromDockerfileInFileSystem("../realguardio-security-system-service/Dockerfile-local")
            .withNetwork(eventuateKafkaCluster.network)
            .withNetworkAliases("security-system-service")
            .withDatabase(securityDatabase)
            .withKafka(kafka)
            .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
            .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "update")
            .withReuse(true)
            .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC security-service:"));

    private static final EventuateCdcContainer cdc = new EventuateCdcContainer()
        .withKafka(kafka)
        .withKafkaLeadership()
        .withTramPipeline(customerDatabase)
        .withTramPipeline(securityDatabase)
        .withTramPipeline(orchestrationDatabase)
        .withReuse(false)
        .withExposedPorts(8080)
        .dependsOn(customerService, securitySystemService, orchestrationService)
        .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("cdc:"));

    private String authToken;
    
    @BeforeAll
    static void startContainers() {
        configureRestAssured();

        kafka.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("kafka:"));
        Startables.deepStart(
            kafka,
            iamService,
            customerService,
            orchestrationService,
            securitySystemService,
            cdc
        ).join();
    }
    
    private static void configureRestAssured() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
    }
    
    @BeforeEach
    void setup() {
        authToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
    }
    
    @Test 
    void dockerComposeShouldStart() {
        assertThat(customerService.isRunning()).isTrue();
        assertThat(orchestrationService.isRunning()).isTrue();
        assertThat(securitySystemService.isRunning()).isTrue();
        assertThat(iamService.isRunning()).isTrue();
        
        assertThat(customerService.getFirstMappedPort()).isPositive();
        assertThat(orchestrationService.getFirstMappedPort()).isPositive();
        assertThat(securitySystemService.getFirstMappedPort()).isPositive();
        assertThat(iamService.getFirstMappedPort()).isPositive();
    }
    
    @Test
    void shouldCreateCustomerAndSecuritySystem() {
        CreateCustomerResponse customerResponse = createCustomerResponse();
        long customerId = customerResponse.customer().id();

        assertThat(customerId).isGreaterThan(0);

        String locationName = "Office Main Entrance";

        // Start the saga via REST API
        String sagaRequestJson = """
		{
			"customerId": %d,
			"locationName": "%s"
		}
		""".formatted(customerId, locationName);

        logger.info("Starting CreateSecuritySystemSaga for customer {} and location {}", customerId, locationName);

        CreateSecuritySystemResponse createResponse =  RestAssured.given()
            .baseUri("http://localhost:" + orchestrationService.getFirstMappedPort())
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(sagaRequestJson)
            .when()
            .post("/securitysystems")
            .then()
            .statusCode(201) // Created - based on controller implementation
            .extract()
            .body()
            .as(CreateSecuritySystemResponse.class)
            ;

    }

    private CreateCustomerResponse createCustomerResponse() {
        // Arrange - Create unique test data
        String uniqueEmail = "admin-" + UUID.randomUUID() + "@example.com";


        // Act - Create customer
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
            "Test Customer",
            new PersonDetails(new PersonName("Admin", "User"), new EmailAddress(uniqueEmail)));

        CreateCustomerResponse customerResponse = RestAssured.given()
            .baseUri("http://localhost:" + customerService.getFirstMappedPort())
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(customerRequest)
            .when()
            .post("/customers")
            .then()
            .statusCode(200)
            .extract()
            .as(CreateCustomerResponse.class);
        return customerResponse;
    }
}