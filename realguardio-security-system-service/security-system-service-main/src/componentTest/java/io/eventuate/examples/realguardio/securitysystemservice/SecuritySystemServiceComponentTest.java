package io.eventuate.examples.realguardio.securitysystemservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.UpdateCreationFailedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationNoted;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.spring.testing.kafka.producer.EventuateKafkaTestCommandProducerConfiguration;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.restassured.RestAssured;
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
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;

@SpringBootTest(classes = SecuritySystemServiceComponentTest.TestConfiguration.class)
public class SecuritySystemServiceComponentTest {

	protected static Logger logger = LoggerFactory.getLogger(SecuritySystemServiceComponentTest.class);
	private String replyTo;

	@Configuration
	@EnableAutoConfiguration
	@Import({
			EventuateKafkaTestCommandProducerConfiguration.class,
			CommandOutboxTestSupportConfiguration.class,
			EventuateTramFlywayMigrationConfiguration.class,
	})
	static class TestConfiguration {
	}

	public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("security-system-service-tests");

	public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
			.withNetworkAliases("kafka")
			.withReuse(false)
			;

	public static EventuateDatabaseContainer<?> database = DatabaseContainerFactory.makeVanillaDatabaseContainer()
			.withNetwork(eventuateKafkaCluster.network)
			.withNetworkAliases("database")
			.withReuse(false)
			;

	public static AuthorizationServerContainerForServiceContainers iamService = new AuthorizationServerContainerForServiceContainers()
			.withUserDb()
			.withNetwork(eventuateKafkaCluster.network)
			.withNetworkAliases("iam-service")
			.withReuse(false)
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
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC security-system-service:"));
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
				.get("/securitysystems")
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
				.get("/securitysystems")
				.then()
				.statusCode(200);
	}

	@Test
	void shouldHandleCreateSecuritySystemCommand() throws Exception {
		String locationName = "Main Office Entrance";

		CreateSecuritySystemCommand command = new CreateSecuritySystemCommand(locationName);

		logger.info("Sending CreateSecuritySystemCommand: {}", command);
		String commandId = commandProducer.send("security-system-service",
				command,
				replyTo, Collections.emptyMap());
		logger.info("Sent CreateSecuritySystemCommand with id: {}.. waiting for reply", commandId);

		// Wait for and verify reply
		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
			commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
			// Note: Further verification of reply content would require access to the actual reply,
			// which is not directly available through CommandOutboxTestSupport
			logger.info("SecuritySystemCreated reply received");
		});
	}

// Claude generated fake securitySystemID so these tests fail
//
//	@Test
//	void shouldHandleNoteLocationCreatedCommand() throws Exception {
//		// First create a security system
//		String locationName = "Test Location for Note";
//		CreateSecuritySystemCommand createCommand = new CreateSecuritySystemCommand(locationName);
//
//		logger.info("Creating SecuritySystem first");
//		String createCommandId = commandProducer.send("security-system-service",
//				createCommand,
//				replyTo, Collections.emptyMap());
//
//		// Wait for creation to complete
//		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
//			commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
//		});
//
//		// Use a different replyTo for the next command to avoid confusion
//		String noteReplyTo = UUID.randomUUID().toString();
//
//		// Now send NoteLocationCreatedCommand with a test security system ID
//		long securitySystemId = System.currentTimeMillis();
//		long locationId = System.currentTimeMillis() + 1000;
//		NoteLocationCreatedCommand noteCommand = new NoteLocationCreatedCommand(securitySystemId, locationId);
//
//		logger.info("Sending NoteLocationCreatedCommand: {}", noteCommand);
//		String noteCommandId = commandProducer.send("security-system-service",
//				noteCommand,
//				noteReplyTo, Collections.emptyMap());
//		logger.info("Sent NoteLocationCreatedCommand with id: {}.. waiting for reply", noteCommandId);
//
//		// Wait for and verify reply
//		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
//			commandOutboxTestSupport.assertCommandReplyMessageSent(noteReplyTo);
//			logger.info("Location noted successfully for SecuritySystem ID: {}", noteCommandId);
//		});
//	}
//
//	@Test
//	void shouldHandleUpdateCreationFailedCommand() throws Exception {
//		// First create a security system
//		String locationName = "Test Location for Failure";
//		CreateSecuritySystemCommand createCommand = new CreateSecuritySystemCommand(locationName);
//
//		logger.info("Creating SecuritySystem first");
//		String createCommandId = commandProducer.send("security-system-service",
//				createCommand,
//				replyTo, Collections.emptyMap());
//
//		// Wait for creation to complete
//		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
//			commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
//		});
//
//		// Use a different replyTo for the next command
//		String failReplyTo = UUID.randomUUID().toString();
//
//		// Now send UpdateCreationFailedCommand with a test security system ID
//		long securitySystemId = System.currentTimeMillis();
//		String rejectionReason = "Customer not found";
//		UpdateCreationFailedCommand failCommand = new UpdateCreationFailedCommand(securitySystemId, rejectionReason);
//
//		logger.info("Sending UpdateCreationFailedCommand: {}", failCommand);
//		String failCommandId = commandProducer.send("security-system-service",
//				failCommand,
//				failReplyTo, Collections.emptyMap());
//		logger.info("Sent UpdateCreationFailedCommand with id: {}.. waiting for reply", failReplyTo);
//
//		// Wait for and verify reply
//		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
//			commandOutboxTestSupport.assertCommandReplyMessageSent(failCommandId);
//			logger.info("Creation failure updated successfully for SecuritySystem ID: {}", securitySystemId);
//		});
//	}
}
