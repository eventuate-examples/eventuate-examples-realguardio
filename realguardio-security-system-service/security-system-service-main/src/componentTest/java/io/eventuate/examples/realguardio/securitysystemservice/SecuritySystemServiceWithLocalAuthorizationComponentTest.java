package io.eventuate.examples.realguardio.securitysystemservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.BuildArgsResolver;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.eventuate.tram.testing.producer.kafka.commands.DirectToKafkaCommandProducer;
import io.eventuate.tram.testing.producer.kafka.commands.EnableDirectToKafkaCommandProducer;
import io.eventuate.tram.testing.producer.kafka.events.DirectToKafkaDomainEventPublisher;
import io.eventuate.tram.testing.producer.kafka.events.EnableDirectToKafkaDomainEventPublisher;
import io.realguardio.osointegration.ososervice.OsoServiceConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.eventuate.util.test.async.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Component test for the security system service with the OsoLocalSecuritySystemLocation profile active.
 *
 * This test verifies:
 * 1. Security systems are created with correct locationId
 * 2. No SecuritySystemAssignedToLocation events are published (event suppression)
 * 3. Authorization works correctly using local data bindings
 */
@SpringBootTest(classes = SecuritySystemServiceWithLocalAuthorizationComponentTest.TestConfiguration.class)
@ActiveProfiles("UseOsoService")
public class SecuritySystemServiceWithLocalAuthorizationComponentTest {

	protected static Logger logger = LoggerFactory.getLogger(SecuritySystemServiceWithLocalAuthorizationComponentTest.class);
	private String replyTo;

	@Configuration
	@EnableAutoConfiguration(exclude = {JpaRepositoriesAutoConfiguration.class, FlywayAutoConfiguration.class})
	@Import({
			CommandOutboxTestSupportConfiguration.class,
			OsoServiceConfiguration.class
	})
	@EnableDirectToKafkaCommandProducer
	@EnableDirectToKafkaDomainEventPublisher
	static class TestConfiguration {

		@Bean
		UserService userService() {
			return new UserServiceImpl();
		}

	}

	// Use a unique network name to avoid conflicts with other tests
	public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("ss-local-auth-tests");

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

	public static OsoTestContainer osoDevServer = new OsoTestContainer()
			.withNetwork(eventuateKafkaCluster.network)
			.withNetworkAliases("oso-dev-server")
			.withReuse(false)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-dev-server:"));

	// Service container with OsoLocalSecuritySystemLocation profile
	public static GenericContainer<?> service =
			new ServiceContainer(new ImageFromDockerfile()
					.withFileFromPath(".", Paths.get("../..").toAbsolutePath())
					.withDockerfilePath("realguardio-security-system-service/Dockerfile-local")
					.withBuildArgs(BuildArgsResolver.buildArgs()))
			.withNetwork(eventuateKafkaCluster.network)
			.withDatabase(database)
			.withKafka(kafka)
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
			.withEnv("OSO_URL", "http://oso-dev-server:8080")
			.withEnv("OSO_AUTH", "e_0123456789_12345_osotesttoken01xiIn")
			// Key difference: activate OsoLocalSecuritySystemLocation profile
			.withEnv("SPRING_PROFILES_ACTIVE", "test,postgres,UseOsoService,OsoLocalSecuritySystemLocation")
			.withReuse(true)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC security-system-service:"));
		;

	@Autowired
	private DirectToKafkaCommandProducer commandProducer;

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

	@Autowired
	private RealGuardOsoFactManager realGuardOsoFactManager;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		Startables.deepStart(service, iamService, osoDevServer).join();

		kafka.registerProperties(registry::add);
		database.registerProperties(registry::add);
		osoDevServer.addProperties(registry);

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
	void shouldCreateSecuritySystemWithoutPublishingSecuritySystemAssignedToLocationEvent() throws Exception {
		Long locationId = System.currentTimeMillis();
		String locationName = "Local Auth Test Location";

		CreateSecuritySystemCommand command = new CreateSecuritySystemCommand(locationId, locationName);

		logger.info("Sending CreateSecuritySystemCommand with locationId: {}", locationId);
		String commandId = commandProducer.send("security-system-service",
				command,
				replyTo, Collections.emptyMap());
		logger.info("Sent CreateSecuritySystemCommand with id: {}.. waiting for reply", commandId);

		// Wait for and verify reply
		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
			commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
			logger.info("SecuritySystemCreated reply received");
		});

		// Small wait to ensure processing is complete
		Thread.sleep(500);

		// Verify by checking the message outbox - SecuritySystemAssignedToLocation should NOT be in the outbox
		// The event, if published, would be in the message table
		// creation_time is stored as epoch milliseconds (bigint)
		long oneMinuteAgo = System.currentTimeMillis() - 60000;
		List<String> securitySystemEvents = jdbcTemplate.queryForList(
				"SELECT payload FROM message " +
				"WHERE payload LIKE '%SecuritySystemAssignedToLocation%' " +
				"AND creation_time > ?",
				String.class,
				oneMinuteAgo
		);

		// When OsoLocalSecuritySystemLocation profile is active, no SecuritySystemAssignedToLocation
		// events should be published
		assertThat(securitySystemEvents)
				.as("No SecuritySystemAssignedToLocation events should be published when OsoLocalSecuritySystemLocation profile is active")
				.isEmpty();

		logger.info("Verified: No SecuritySystemAssignedToLocation event was published");
	}

	@Test
	void shouldAuthorizeArmOperationWithLocalDataBindings() throws Exception {
		Long locationId = System.currentTimeMillis();
		String locationName = "Authorization Test Location";
		String company = "acme" + System.currentTimeMillis();
		String customerEmployeeEmail = "employee%s@realguard.io".formatted(System.currentTimeMillis());

		// Create security system
		CreateSecuritySystemCommand command = new CreateSecuritySystemCommand(locationId, locationName);
		commandProducer.send("security-system-service", command, replyTo, Collections.emptyMap());

		eventually(30, 500, TimeUnit.MILLISECONDS, () -> {
			commandOutboxTestSupport.assertCommandReplyMessageSent(replyTo);
		});

		// Get the security system ID from the database (query by locationId)
		Long securitySystemId = jdbcTemplate.queryForObject(
				"SELECT id FROM security_system WHERE location_id = ?",
				Long.class,
				locationId
		);
		logger.info("Created security system with ID: {}", securitySystemId);

		// Set up Oso facts for authorization (Location-Customer, User-Role)
		// These facts are still stored in Oso Cloud; only SecuritySystem-Location uses local bindings
		realGuardOsoFactManager.createLocationForCustomer(String.valueOf(locationId), company);
		realGuardOsoFactManager.createRoleAtLocation(customerEmployeeEmail, String.valueOf(locationId), "SECURITY_SYSTEM_ARMER");

		// Create the customer employee user
		userService.createCustomerEmployeeUser(customerEmployeeEmail);

		// Get JWT token for the customer employee
		String accessToken = JwtTokenHelper.getJwtTokenForUser(
				iamService.getFirstMappedPort(),
				"iam-service:9000",
				customerEmployeeEmail,
				"password"
		);

		// Small wait for Oso facts to propagate
		Thread.sleep(1000);

		// Arm the security system - this tests the local authorization with SecuritySystem-Location binding
		// The authorization should work using local data bindings for SecuritySystem-Location
		RestAssured.given()
				.baseUri(String.format("http://localhost:%d", service.getFirstMappedPort()))
				.header("Authorization", "Bearer " + accessToken)
				.contentType("application/json")
				.body("{\"action\": \"ARM\"}")
				.when()
				.put("/securitysystems/{id}", securitySystemId)
				.then()
				.statusCode(200);

		logger.info("Customer employee successfully armed security system {} with local authorization", securitySystemId);
	}
}
