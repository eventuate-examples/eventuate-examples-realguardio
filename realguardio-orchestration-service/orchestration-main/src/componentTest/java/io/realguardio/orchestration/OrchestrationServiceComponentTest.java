package io.realguardio.orchestration;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.testing.kafka.producer.EventuateKafkaTestCommandProducerConfiguration;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemResponse;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
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
			EventuateKafkaTestCommandProducerConfiguration.class,
			ComponentTestSupportConfiguration.class
	})
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

		long customerId = 12345L;
		String locationName = "Office Main Entrance";
		
		// Start the saga via REST API
		String sagaRequestJson = """
		{
			"customerId": %d,
			"locationName": "%s"
		}
		""".formatted(customerId, locationName);

		logger.info("Starting CreateSecuritySystemSaga for customer {} and location {}", customerId, locationName);
		
		CompletableFuture<CreateSecuritySystemResponse> createResponse = CompletableFuture.supplyAsync(() ->
			RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(sagaRequestJson)
				.when()
				.post("/securitysystems")
				.then()
				.statusCode(201) // Created - based on controller implementation
				.extract()
				.body()
				.as(CreateSecuritySystemResponse.class)
		);

		Message createCommandMessage = componentTestSupport.assertThatCommandMessageSent(CreateSecuritySystemCommand.class, securitySystemServiceChannel);

		long securitySystemId = System.currentTimeMillis();

		componentTestSupport.sendReply(createCommandMessage, new SecuritySystemCreated(securitySystemId));

		assertThat(createResponse.get().securitySystemId()).isEqualTo(securitySystemId);
	}



}