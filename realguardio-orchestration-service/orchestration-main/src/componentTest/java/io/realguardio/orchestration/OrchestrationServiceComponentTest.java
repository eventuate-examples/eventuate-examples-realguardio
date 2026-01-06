package io.realguardio.orchestration;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.testing.producer.kafka.replies.EnableDirectToKafkaCommandReplyProducer;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemResponse;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.ValidateLocationCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationValidated;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationAlreadyHasSecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OrchestrationServiceComponentTest.TestConfiguration.class)
public class OrchestrationServiceComponentTest {

	protected static Logger logger = LoggerFactory.getLogger(OrchestrationServiceComponentTest.class);

	@Configuration
	@EnableAutoConfiguration
	@Import({
			ComponentTestSupportConfiguration.class
	})
	@EnableDirectToKafkaCommandReplyProducer
	static class TestConfiguration {

	}


	public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("orchestration-service-tests");

	public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
			.withNetworkAliases("kafka")
			.withReuse(true)
			;

	public static EventuateDatabaseContainer<?> database = DatabaseContainerFactory.makeVanillaDatabaseContainer()
			.withNetwork(eventuateKafkaCluster.network)
			.withNetworkAliases("database")
			.withReuse(true)
			;

	public static AuthorizationServerContainerForServiceContainers iamService = new AuthorizationServerContainerForServiceContainers()
			.withUserDb()
			.withNetwork(eventuateKafkaCluster.network)
			.withNetworkAliases("iam-service")
			.withReuse(true)
			;

	private static String securitySystemServiceChannel = "security-system-service-" + System.currentTimeMillis();
	private static String customerServiceChannel = "customer-service-" + System.currentTimeMillis();

	// Orchestration Service under test
	public static GenericContainer<?> service =
		ServiceContainer.makeFromDockerfileInFileSystem("../Dockerfile-local")
			.withNetwork(eventuateKafkaCluster.network)
			.withDatabase(database)
			.withKafka(kafka)
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
			.withEnv("SPRING_PROFILES_ACTIVE", "test,postgres")
			.withEnv("SECURITYSYSTEMSERVICE_CHANNEL", securitySystemServiceChannel) // use unique channel name for each test run
			.withEnv("CUSTOMERSERVICE_CHANNEL", customerServiceChannel)
			.withReuse(true)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC orchestration-service:"));
		;


	@Autowired
	private ComponentTestSupport componentTestSupport;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		Startables.deepStart(service, iamService).join();

		kafka.registerProperties(registry::add);
		database.registerProperties(registry::add);

		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
				() -> "http://localhost:" + iamService.getFirstMappedPort());
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
				() -> "http://localhost:" + iamService.getFirstMappedPort() + "/oauth2/jwks");
	}

	@Test
	void shouldStart() {
		assertThat(service.isRunning()).isTrue();
		assertThat(service.getFirstMappedPort()).isNotNull();
	}

	@Test
	void healthEndpointReturnsOk() {
		RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.when()
				.get("/actuator/health")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.asString()
				.contains("UP");
	}

	@Test
	void shouldReturn200WithValidJwtToken() {
		String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
		
		RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/actuator/info")
				.then()
				.statusCode(200);
	}

	@Test
	void shouldOrchestrateCreateSecuritySystemSaga() throws Exception {
		String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
		String baseUri = String.format("http://localhost:%d", service.getFirstMappedPort());

		long locationId = System.currentTimeMillis();
		String locationName = "Office Location";
		long customerId = 54321L;
		long securitySystemId = System.currentTimeMillis() + 1;

		// Start the saga via REST API with locationId
		String sagaRequestJson = """
		{
			"locationId": %d
		}
		""".formatted(locationId);

		logger.info("Starting CreateSecuritySystemSaga for locationId {}", locationId);

		CompletableFuture<CreateSecuritySystemResponse> createResponse = CompletableFuture.supplyAsync(() ->
			RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(sagaRequestJson)
				.when()
				.post("/securitysystems")
				.then()
				.statusCode(201)
				.extract()
				.body()
				.as(CreateSecuritySystemResponse.class)
		);

		// Step 1: Intercept ValidateLocationCommand with matching locationId
		Message validateCommandMessage = componentTestSupport.assertThatCommandMessageSent(
				ValidateLocationCommand.class, customerServiceChannel,
				cmd -> cmd.locationId().equals(locationId));

		// Reply with LocationValidated
		componentTestSupport.sendReply(validateCommandMessage, ValidateLocationCommand.class, new LocationValidated(locationId, locationName, customerId));

		// Step 2: Intercept CreateSecuritySystemCommand with matching locationId
		Message createCommandMessage = componentTestSupport.assertThatCommandMessageSent(
				CreateSecuritySystemCommand.class, securitySystemServiceChannel,
				cmd -> cmd.locationId().equals(locationId));

		// Reply with SecuritySystemCreated
		componentTestSupport.sendReply(createCommandMessage, CreateSecuritySystemCommand.class, new SecuritySystemCreated(securitySystemId));

		// Verify response
		assertThat(createResponse.get().securitySystemId()).isEqualTo(securitySystemId);
	}

	@Test
	void shouldReturn404WhenLocationNotFound() throws Exception {
		String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
		String baseUri = String.format("http://localhost:%d", service.getFirstMappedPort());

		long nonExistentLocationId = System.currentTimeMillis();

		// Start the saga via REST API with locationId that doesn't exist
		String sagaRequestJson = """
		{
			"locationId": %d
		}
		""".formatted(nonExistentLocationId);

		logger.info("Starting CreateSecuritySystemSaga for non-existent locationId {}", nonExistentLocationId);

		CompletableFuture<Integer> statusCodeFuture = CompletableFuture.supplyAsync(() ->
			RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(sagaRequestJson)
				.when()
				.post("/securitysystems")
				.then()
				.extract()
				.statusCode()
		);

		// Step 1: Intercept ValidateLocationCommand with matching locationId
		Message validateCommandMessage = componentTestSupport.assertThatCommandMessageSent(
				ValidateLocationCommand.class, customerServiceChannel,
				cmd -> cmd.locationId().equals(nonExistentLocationId));

		// Reply with LocationNotFound (failure)
		componentTestSupport.sendReply(validateCommandMessage, ValidateLocationCommand.class, new LocationNotFound());

		// Verify response is 404 Not Found
		assertThat(statusCodeFuture.get()).isEqualTo(404);
	}

	@Test
	void shouldReturn409WhenLocationAlreadyHasSecuritySystem() throws Exception {
		String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
		String baseUri = String.format("http://localhost:%d", service.getFirstMappedPort());

		long locationId = System.currentTimeMillis();
		String locationName = "Office Location";
		long customerId = 54321L;

		// Start the saga via REST API with locationId
		String sagaRequestJson = """
		{
			"locationId": %d
		}
		""".formatted(locationId);

		logger.info("Starting CreateSecuritySystemSaga for locationId {} (already has security system)", locationId);

		CompletableFuture<Integer> statusCodeFuture = CompletableFuture.supplyAsync(() ->
			RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(sagaRequestJson)
				.when()
				.post("/securitysystems")
				.then()
				.extract()
				.statusCode()
		);

		// Step 1: Intercept ValidateLocationCommand with matching locationId
		Message validateCommandMessage = componentTestSupport.assertThatCommandMessageSent(
				ValidateLocationCommand.class, customerServiceChannel,
				cmd -> cmd.locationId().equals(locationId));

		// Reply with LocationValidated (location exists)
		componentTestSupport.sendReply(validateCommandMessage, ValidateLocationCommand.class, new LocationValidated(locationId, locationName, customerId));

		// Step 2: Intercept CreateSecuritySystemCommand with matching locationId
		Message createCommandMessage = componentTestSupport.assertThatCommandMessageSent(
				CreateSecuritySystemCommand.class, securitySystemServiceChannel,
				cmd -> cmd.locationId().equals(locationId));

		// Reply with LocationAlreadyHasSecuritySystem (failure - constraint violation)
		componentTestSupport.sendReply(createCommandMessage, CreateSecuritySystemCommand.class, new LocationAlreadyHasSecuritySystem(locationId));

		// Verify response is 409 Conflict
		assertThat(statusCodeFuture.get()).isEqualTo(409);
	}

}