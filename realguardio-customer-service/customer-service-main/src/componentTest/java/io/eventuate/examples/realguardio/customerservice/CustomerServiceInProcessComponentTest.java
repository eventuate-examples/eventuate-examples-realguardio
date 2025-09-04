package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.CreateLocationWithSecuritySystemCommand;
import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForLocalTests;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.lifecycle.Startables;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CustomerServiceInProcessComponentTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomerServiceInProcessComponentTest extends AbstractCustomerServiceComponentTest {


	@TestConfiguration
	@Import({AbstractConfig.class})
	@EnableAutoConfiguration
	static class Config {

		@Bean
		DirectToKafkaCommandProducer directToKafkaCommandProducer(@Value("${eventuatelocal.kafka.bootstrap.servers}") String bootstrapServer) {
			return new DirectToKafkaCommandProducer(bootstrapServer);
		}
	}

	@Autowired
	private DirectToKafkaCommandProducer directToKafkaCommandProducer;

	@LocalServerPort
	private int port;

	static {
		iamService = new AuthorizationServerContainerForLocalTests()
				.withUserDb()
				.withNetwork(eventuateKafkaCluster.network)
				.withNetworkAliases("iam-service")
				.withReuse(true);
	}

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		Startables.deepStart(kafka, database, iamService).join();

		kafka.registerProperties(registry::add);
		database.registerProperties(registry::add);

		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
				() -> "http://localhost:" + iamService.getFirstMappedPort());
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
				() -> "http://localhost:" + iamService.getFirstMappedPort() + "/oauth2/jwks");
	}

	@BeforeEach
	void setupReplyConsumer() {
		baseUri = String.format("http://localhost:%d", port);
	}

	@Override
	protected String sendCommand(CreateLocationWithSecuritySystemCommand command, String replyTo) {
		return directToKafkaCommandProducer.send("customer-service",
				command,
				replyTo, Collections.emptyMap());
	}


	@Test
	void shouldHandleCreateLocationWithSecuritySystemCommand() throws Exception {
		String realGuardIOAdminAccessToken = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort());

		EmailAddress adminUser = Uniquifier.uniquify(new EmailAddress("admin@example.com"));

		CustomerSummary customerSummary = createCustomer(adminUser, realGuardIOAdminAccessToken);

		long securitySystemId = System.currentTimeMillis();
		var locationId = createLocationForSecuritySystem(customerSummary.customerId(), securitySystemId);

    assertThat(getRolesForLocation(realGuardIOAdminAccessToken, locationId).getRoles()).isEmpty();

		String companyAdminAccessToken = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort(), null, adminUser.email(), "password");

		assertThat(getRolesForLocation(companyAdminAccessToken, locationId).getRoles()).isEmpty();

		assignDisarmRoleToSelfAtLocation(companyAdminAccessToken, customerSummary, locationId);

		assertThat(getRolesForLocation(companyAdminAccessToken, locationId).getRoles()).contains("DISARM");

	}

	private void assignDisarmRoleToSelfAtLocation(String companyAdminAccessToken, CustomerSummary customerSummary, Long locationId) {
		String requestBody = """
			{
				"employeeId": %d,
				"locationId": %d,
				"roleName": "DISARM"
			}
			""".formatted(customerSummary.employeeId(), locationId);

		RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + companyAdminAccessToken)
				.contentType("application/json")
				.body(requestBody)
				.when()
				.put("/customers/" + customerSummary.customerId() + "/location-roles")
				.then()
				.statusCode(200);
	}


}