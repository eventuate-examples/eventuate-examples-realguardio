package io.eventuate.examples.realguardio.securitysystemservice;

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
class SecuritySystemServiceComponentTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

	@Container
	static GenericContainer<?> iamService = new GenericContainer<>("eventuate-examples-realguardio-realguardio-iam-service:latest")
			.withExposedPorts(9000)
			.withEnv("SPRING_PROFILES_ACTIVE", "realguardio");

	@Container
	static SecuritySystemServiceContainer serviceContainer = new SecuritySystemServiceContainer(postgres, iamService);

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
		// TODO: This test will pass once SecuritySystemController is updated to use SecuritySystemService with JPA
		// First, get a JWT token from the IAM service
		String iamServiceUrl = String.format("http://localhost:%d", iamService.getMappedPort(9000));
		WebClient iamClient = WebClient.builder()
				.baseUrl(iamServiceUrl)
				.build();
		
		// Get JWT token using client credentials flow
		// The client_id and client_secret are configured in the IAM service
		// Use Host header to ensure JWT issuer matches what the service expects
		String tokenResponse = iamClient.post()
				.uri("/oauth2/token")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("realguardio-client:secret-rg".getBytes()))
				.header("Host", "iam-service:9000")
				.bodyValue("grant_type=client_credentials")
				.retrieve()
				.bodyToMono(String.class)
				.block(Duration.ofSeconds(10));
		
		// Extract the access token from response
		// Simple extraction - in production use proper JSON parsing
		String accessToken = tokenResponse.split("\"access_token\":\"")[1].split("\"")[0];
		
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
