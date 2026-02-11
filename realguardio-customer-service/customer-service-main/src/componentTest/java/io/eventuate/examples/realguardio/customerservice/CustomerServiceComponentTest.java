package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.restapi.RolesResponse;
import io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForServiceContainers;
import io.eventuate.testcontainers.service.BuildArgsResolver;
import io.eventuate.testcontainers.service.ServiceContainer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.lifecycle.Startables;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CustomerServiceComponentTest.Config.class)
public class CustomerServiceComponentTest extends AbstractCustomerServiceComponentTest {


	@Configuration
	@Import({AbstractConfig.class})
	static class Config {
	}

	static {
		makeContainers(CustomerServiceComponentTest.class.getSimpleName());
		iamService = new AuthorizationServerContainerForServiceContainers()
				.withUserDb()
				.withNetwork(eventuateKafkaCluster.network)
				.withNetworkAliases("iam-service")
				.withReuse(false); // Cant re
	}

	public static GenericContainer<?> service =

		new ServiceContainer(new ImageFromDockerfile()
                .withFileFromPath(".", Paths.get("../..").toAbsolutePath())  // Context: parent directory
                .withDockerfilePath("realguardio-customer-service/Dockerfile-local")  // Dockerfile path
                .withBuildArgs(BuildArgsResolver.buildArgs()))
			.withNetwork(eventuateKafkaCluster.network)
			.withDatabase(database)
			.withKafka(kafka)
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", "http://iam-service:9000")
			.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", "http://iam-service:9000/oauth2/jwks")
			.withEnv("SPRING_PROFILES_ACTIVE", "test,postgres")
			.withReuse(true)
			.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("SVC customer-service:"));
		;


	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		Startables.deepStart(kafka, database, service, iamService).join();

		kafka.registerProperties(registry::add);
		database.registerProperties(registry::add);

		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
				() -> "http://localhost:" + iamService.getFirstMappedPort());
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
				() -> "http://localhost:" + iamService.getFirstMappedPort() + "/oauth2/jwks");
	}

	@BeforeEach
	void setupReplyConsumer() {
		baseUri = String.format("http://localhost:%d", service.getFirstMappedPort());
	}

	@Test
	void shouldStart() {
		assertThat(service.isRunning()).isTrue();
		assertThat(service.getFirstMappedPort()).isNotNull();
	}

	@Test
	void healthEndpointReturnsOk() {
		RestAssured.given()
				.baseUri(baseUri)
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
				.baseUri(baseUri)
				.when()
				.get("/customers")
				.then()
				.statusCode(401);
	}

	@Test
	void shouldReturn200WithValidJwtToken() {
		String accessToken = getAccessTokenForRealGuardIoAdmin();

		RestAssured.given()
				.baseUri(baseUri)
				.header("Authorization", "Bearer " + accessToken)
				.when()
				.get("/customers")
				.then()
				.statusCode(200);
	}

	@Test
	void shouldCreateLocationViaRestApi() throws Exception {
		String realGuardIOAdminAccessToken = getAccessTokenForRealGuardIoAdmin();

		EmailAddress adminUser = Uniquifier.uniquify(new EmailAddress("admin@example.com"));

		CustomerSummary customerSummary = createCustomer(adminUser, realGuardIOAdminAccessToken);

		String companyAdminAccessToken = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort(), null, adminUser.email(), "password");

		var locationId = createLocation(customerSummary.customerId(), companyAdminAccessToken);

		RolesResponse rolesResponse = getRolesForLocation(realGuardIOAdminAccessToken, locationId);

		assertThat(rolesResponse.getRoles()).isEmpty();
	}


}