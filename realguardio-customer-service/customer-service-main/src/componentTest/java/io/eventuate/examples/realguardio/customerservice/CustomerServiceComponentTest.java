package io.eventuate.examples.realguardio.customerservice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class CustomerServiceComponentTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

	@Container
	static GenericContainer<?> iamService = new GenericContainer<>("eventuate-examples-realguardio-realguardio-iam-service:latest")
			.withExposedPorts(9000)
			.withEnv("SPRING_PROFILES_ACTIVE", "realguardio");

	@Container
	static CustomerServiceContainer serviceContainer = new CustomerServiceContainer(postgres, iamService);

	@Test
	void containerStartsSuccessfully() {
		assertThat(serviceContainer.isRunning()).isTrue();
		assertThat(serviceContainer.getMappedServicePort()).isNotNull();
	}

	@Test
	void healthEndpointReturnsOk() {
		WebClient webClient = serviceContainer.createWebClient();
		
		String response = webClient.get()
				.uri("/actuator/health")
				.retrieve()
				.bodyToMono(String.class)
				.block(Duration.ofSeconds(10));

		assertThat(response).contains("UP");
	}

	@Test
	void shouldReturn401WhenNoAuthenticationProvided() {
		WebClient webClient = serviceContainer.createWebClient();
		
		ClientResponse response = webClient.get()
				.uri("/securitysystems")
				.exchangeToMono(Mono::just)
				.block(Duration.ofSeconds(10));
		
		assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}
	
	@Test
	void shouldReturn200WithValidJwtToken() {
		// Get JWT token using the helper with Host header for component tests
		String accessToken = JwtTokenHelper.getJwtTokenWithHostHeader(iamService.getMappedPort(9000));
		
		// Now make request to our service with the JWT token
		WebClient webClient = serviceContainer.createWebClient();
		
		ClientResponse response = webClient.get()
				.uri("/securitysystems")
				.header("Authorization", "Bearer " + accessToken)
				.exchangeToMono(Mono::just)
				.block(Duration.ofSeconds(10));
		
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
	}
}
