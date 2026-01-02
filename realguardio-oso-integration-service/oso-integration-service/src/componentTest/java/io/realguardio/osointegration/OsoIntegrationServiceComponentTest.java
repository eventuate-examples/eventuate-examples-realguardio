package io.realguardio.osointegration;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployeeAssignedCustomerRole;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.LocationCreatedForCustomer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.SecuritySystemAssignedToLocation;
import io.eventuate.examples.realguardio.customerservice.domain.CustomerEmployeeAssignedLocationRole;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.eventuate.tram.spring.consumer.common.TramNoopDuplicateMessageDetectorConfiguration;
import io.eventuate.tram.testing.producer.kafka.events.DirectToKafkaDomainEventPublisher;
import io.eventuate.tram.testing.producer.kafka.events.EnableDirectToKafkaDomainEventPublisher;
import io.realguardio.osointegration.ososervice.OsoService;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import io.realguardio.osointegration.testcontainer.OsoTestContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.util.concurrent.ExecutionException;

import static io.eventuate.util.test.async.Eventually.eventually;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OsoIntegrationServiceComponentTest.TestConfiguration.class)
public class OsoIntegrationServiceComponentTest {

	protected static Logger logger = LoggerFactory.getLogger(OsoIntegrationServiceComponentTest.class);

	@Configuration
	@EnableAutoConfiguration
	@Import({
			TramNoopDuplicateMessageDetectorConfiguration.class
	})
	@ComponentScan(basePackageClasses = OsoService.class)
	@EnableDirectToKafkaDomainEventPublisher
	static class TestConfiguration {
	}

	public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("oso-integration-service-tests");

	public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
			.withNetworkAliases("kafka")
			.withReuse(false)
			;

	public static OsoTestContainer osoServer = new OsoTestContainer()
			.withNetwork(eventuateKafkaCluster.network)
			.withNetworkAliases("oso-server")
			.withReuse(false)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-server:"));

	;

	public static GenericContainer<?> service =
		ServiceContainer.makeFromDockerfileInFileSystem("../Dockerfile-local")
			.withNetwork(eventuateKafkaCluster.network)
			.withKafka(kafka)
			.withEnv("OSO_URL", "http://oso-server:8080")
			.withEnv("OSO_AUTH", "e_0123456789_12345_osotesttoken01xiIn")
			.withEnv("SPRING_PROFILES_ACTIVE", "test")
			.withReuse(false)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC oso-integration-service:"));
		;

	@Autowired
	private DirectToKafkaDomainEventPublisher domainEventPublisher;

	@Autowired
	private RealGuardOsoAuthorizer realGuardOsoAuthorizer;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		Startables.deepStart(service, osoServer).join();

		kafka.registerProperties(registry::add);
		osoServer.addProperties(registry);
	}

	@Test
	void shouldStart() {
		assertThat(service.isRunning()).isTrue();
		assertThat(service.getFirstMappedPort()).isNotNull();
		assertThat(osoServer.isRunning()).isTrue();
	}

	@Test
	void shouldAuthorizeAliceForCustomerAcme() throws Exception {
		String customerId = "acme-" + System.currentTimeMillis();
		Long aliceId = System.currentTimeMillis();
		Long locationId = System.currentTimeMillis() + 1000;
		Long ss1Id = System.currentTimeMillis() + 2000;
		Long ss2Id = System.currentTimeMillis() + 3000;

		logger.info("Setting up customer {} with location {} and security systems", customerId, locationId);

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new CustomerEmployeeAssignedCustomerRole(aliceId, "SECURITY_SYSTEM_DISARMER"));

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new LocationCreatedForCustomer(locationId));

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new SecuritySystemAssignedToLocation(locationId, ss1Id));

		logger.info("Verifying alice can disarm ss1 but not ss2");

		eventually(() -> {
			boolean authorized = isAuthorized(aliceId.toString(), "disarm", ss1Id.toString());
			assertThat(authorized).isTrue();
		});

		boolean unauthorizedAccess = isAuthorized(aliceId.toString(), "disarm", ss2Id.toString());
		assertThat(unauthorizedAccess).isFalse();
	}

	@Test
	void shouldAuthorizeBobForCustomerFoo() throws Exception {
		String customerId = "foo-" + System.currentTimeMillis();
		Long bobId = System.currentTimeMillis();
		Long locationId = System.currentTimeMillis() + 1000;
		Long ss1Id = System.currentTimeMillis() + 2000;
		Long ss2Id = System.currentTimeMillis() + 3000;

		logger.info("Setting up customer {} with location {} and security system {}", customerId, locationId, ss2Id);

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new CustomerEmployeeAssignedCustomerRole(bobId, "SECURITY_SYSTEM_DISARMER"));

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new LocationCreatedForCustomer(locationId));

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new SecuritySystemAssignedToLocation(locationId, ss2Id));

		logger.info("Verifying bob can disarm ss2 but not ss1");

		eventually(() -> {
			boolean authorized = isAuthorized(bobId.toString(), "disarm", ss2Id.toString());
			assertThat(authorized).isTrue();
		});

		boolean unauthorizedAccess = isAuthorized(bobId.toString(), "disarm", ss1Id.toString());
		assertThat(unauthorizedAccess).isFalse();
	}

	@Test
	void shouldAuthorizeMaryForLocation() throws Exception {
		String customerId = "customer-" + System.currentTimeMillis();
		String maryUserName = "mary-" + System.currentTimeMillis();
		Long locationId = System.currentTimeMillis();
		Long securitySystemId = System.currentTimeMillis() + 1000;

		logger.info("Setting up location {} with role for mary and security system {}", locationId, securitySystemId);

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new LocationCreatedForCustomer(locationId));

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new CustomerEmployeeAssignedLocationRole(maryUserName, locationId, "SECURITY_SYSTEM_DISARMER"));

		domainEventPublisher.publish("io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer", customerId,
				new SecuritySystemAssignedToLocation(locationId, securitySystemId));

		logger.info("Verifying mary can disarm security system at her location");

		eventually(() -> {
			boolean authorized = isAuthorized(maryUserName, "disarm", securitySystemId.toString());
			assertThat(authorized).isTrue();
		});
	}

    private boolean isAuthorized(String user, String action, String securitySystem) {
        try {
            return realGuardOsoAuthorizer.isAuthorized(user, action, securitySystem).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

	@Test
	void shouldAuthorizeWhenSecuritySystemAssignedToLocationEventFromSecuritySystemService() throws Exception {
		String customerId = "customer-ss-" + System.currentTimeMillis();
		String userName = "user-" + System.currentTimeMillis();
		Long locationId = System.currentTimeMillis();
		Long securitySystemId = System.currentTimeMillis() + 1000;

		createLocationForCustomer(customerId, locationId);
		assignDisarmerRoleToUserAtLocation(customerId, userName, locationId);
		publishSecuritySystemAssignedToLocationFromSecuritySystemService(securitySystemId, locationId);

		eventually(() -> {
			assertThat(isAuthorized(userName, "disarm", securitySystemId.toString())).isTrue();
		});
	}

	private void createLocationForCustomer(String customerId, Long locationId) {
		domainEventPublisher.publish(
				"io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer",
				customerId,
				new LocationCreatedForCustomer(locationId));
	}

	private void assignDisarmerRoleToUserAtLocation(String customerId, String userName, Long locationId) {
		domainEventPublisher.publish(
				"io.eventuate.examples.realguardio.customerservice.customermanagement.domain.Customer",
				customerId,
				new CustomerEmployeeAssignedLocationRole(userName, locationId, "SECURITY_SYSTEM_DISARMER"));
	}

	private void publishSecuritySystemAssignedToLocationFromSecuritySystemService(Long securitySystemId, Long locationId) {
		domainEventPublisher.publish(
				"io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem",
				securitySystemId.toString(),
				new io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAssignedToLocation(securitySystemId, locationId));
	}

}
