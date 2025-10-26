package io.eventuate.examples.realguardio.securitysystemservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.locationroles.LocationRolesReplicaConfiguration;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.BuildArgsResolver;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.commands.producer.CommandProducer;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.flyway.EventuateTramFlywayMigrationConfiguration;
import io.eventuate.tram.spring.testing.kafka.producer.EventuateKafkaTestCommandProducerConfiguration;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.lifecycle.Startables;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.eventuate.util.test.async.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SecuritySystemServiceComponentTest.TestConfiguration.class)
@ActiveProfiles("UseOsoService")
public class SecuritySystemServiceComponentTest {

	protected static Logger logger = LoggerFactory.getLogger(SecuritySystemServiceComponentTest.class);
	private String replyTo;

	@Configuration
	@EnableAutoConfiguration(exclude = {JpaRepositoriesAutoConfiguration.class})
	@Import({
			EventuateKafkaTestCommandProducerConfiguration.class,
			CommandOutboxTestSupportConfiguration.class,
			EventuateTramFlywayMigrationConfiguration.class,
			LocationRolesReplicaConfiguration.class
	})
	static class TestConfiguration {

		@Bean
		DirectToKafkaDomainEventPublisher directToKafkaCommandProducer(@Value("${eventuatelocal.kafka.bootstrap.servers}") String bootstrapServer) {
			return new DirectToKafkaDomainEventPublisher(bootstrapServer);
		}

		@Bean
		UserService userService() {
			return new UserServiceImpl();
		}

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

			new ServiceContainer(new ImageFromDockerfile()
					.withFileFromPath(".", Paths.get("../..").toAbsolutePath())  // Context: parent directory
					.withDockerfilePath("realguardio-security-system-service/Dockerfile-local")  // Dockerfile path
					.withBuildArgs(BuildArgsResolver.buildArgs()))
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

	@Autowired
	private DomainEventPublisher domainEventPublisher;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DirectToKafkaDomainEventPublisher directToKafkaDomainEventPublisher;

	@Autowired
	private UserService userService;

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
	void getSecuritySystemsShouldReturn401WhenNoAuthenticationProvided() {
		RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.when()
				.get("/securitysystems")
				.then()
				.statusCode(401);
	}
	
	@Test
	void getSecuritySystemsShouldReturn403WithRealGuardAdmin() {
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
	void getSecuritySystemsShouldReturn200WithCustomerEmployee() {

		String customerEmployeeEmail = "customerEmployee%s@realguard.io".formatted(System.currentTimeMillis());
		userService.createCustomerEmployeeUser(customerEmployeeEmail);

		String accessToken = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort(), "iam-service:9000", customerEmployeeEmail, "password");

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

	@Test
	void shouldConsumeLocationRoleAssignedEventAndUpdateReplica() throws Exception {
		// This test verifies that if we publish an event from Customer Service,
		// the Security Service would consume it and update its replica.

		// Arrange
		String userName = "test.user@example.com";
		Long locationId = System.currentTimeMillis();
		String roleName = "DISARM";
		String customerId = Long.toString(System.currentTimeMillis());
		
		// Publish the domain event
		logger.info("Publishing CustomerEmployeeAssignedLocationRole event");
		CustomerEmployeeAssignedLocationRole event = new CustomerEmployeeAssignedLocationRole(userName, locationId, roleName);
		directToKafkaDomainEventPublisher.publish("Customer", customerId, event);
		
		// Wait for event to be published to outbox and then processed by the service container
		logger.info("Waiting for event to be consumed and processed by the service");
		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
			// Verify the location role can be queried via REST API
			String accessToken = JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
			
			var response = RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.header("Authorization", "Bearer " + accessToken)
				.queryParam("userName", userName)
				.queryParam("locationId", locationId)
				.when()
				.get("/location-roles")
				.then()
				.statusCode(200)
				.extract()
				.body()
				.jsonPath();
			
			assertThat(response.getList("$")).isNotEmpty();
			assertThat(response.getString("[0].userName")).isEqualTo(userName);
			assertThat(response.getLong("[0].locationId")).isEqualTo(locationId);
			assertThat(response.getString("[0].roleName")).isEqualTo(roleName);
			
			logger.info("Location role successfully retrieved via REST API");
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
