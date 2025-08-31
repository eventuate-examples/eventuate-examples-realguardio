package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.spring.testing.kafka.producer.EventuateKafkaTestCommandProducerConfiguration;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.eventuate.util.test.async.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CustomerServiceComponentTest.TestConfiguration.class)
public class CustomerServiceComponentTest {

	protected static Logger logger = LoggerFactory.getLogger(CustomerServiceComponentTest.class);
	private String replyTo;

	@Configuration
	@EnableAutoConfiguration
	@Import({
			EventuateKafkaTestCommandProducerConfiguration.class,
			CommandOutboxTestSupportConfiguration.class
	})
	static class TestConfiguration {
	}

	public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("customer-service-tests");

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

	public static GenericContainer<?> service =

		ServiceContainer.makeFromDockerfileInFileSystem("../Dockerfile-local")
			.withNetwork(eventuateKafkaCluster.network)
			.withDatabase(database)
			.withKafka(kafka)
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
			.withEnv("SPRING_PROFILES_ACTIVE", "test,postgres")
			.withReuse(true)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC customer-service:"));
		;

	@Autowired
	private CommandProducer commandProducer;

	@Autowired
	private CommandOutboxTestSupport commandOutboxTestSupport;

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

	@BeforeEach
	void setupReplyConsumer() {
		this.replyTo = UUID.randomUUID().toString();
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
	void shouldReturn401WhenNoAuthenticationProvided() {
		RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.when()
				.get("/customers")
				.then()
				.statusCode(401);
	}
	
	@Test
	void shouldReturn200WithValidJwtToken() {
		String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
		
		RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/customers")
				.then()
				.statusCode(200);
	}

	@Test
	void shouldHandleCreateLocationWithSecuritySystemCommand() throws Exception {
		String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
		String baseUri = String.format("http://localhost:%d", service.getFirstMappedPort());

		EmailAddress adminUser = Uniquifier.uniquify(new EmailAddress("admin@example.com"));

		// Create customer
		String customerJson = """
        {
            "name": "New Customer",
            "initialAdministrator": {
                "name": {
                    "firstName": "Admin",
                    "lastName": "User"
                },
                "emailAddress": {
                    "email": "%s"
                }
            }
        }
        """.formatted(adminUser);
		
		Integer customerIdAsInteger = RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + accessToken)
				.contentType(ContentType.JSON)
				.body(customerJson)
				.when()
				.post("/customers")
				.then()
				.statusCode(200)
				.extract()
				.path("customer.id");
		long customerId = Long.valueOf(customerIdAsInteger);

		long securitySystemId = System.currentTimeMillis();
		String locationName = "Office Front Door";

		CreateLocationWithSecuritySystemCommand command = new CreateLocationWithSecuritySystemCommand(
				customerId, locationName, securitySystemId);

		logger.info("Sending CreateLocationWithSecuritySystemCommand: {}", command);
		String commandId = commandProducer.send("customer-service",
				command,
				replyTo, Collections.emptyMap());
		logger.info("Sent CreateLocationWithSecuritySystemCommand with id: {}.. waiting for reply", commandId);

		// Wait for and verify reply
		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
			commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
		});
		
	}


}