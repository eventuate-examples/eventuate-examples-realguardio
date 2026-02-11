package io.eventuate.examples.realguardio.securitysystemservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class UserServiceImpl implements UserService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String iamServiceBaseUrl;

    private final WebClient webClient = WebClient.builder().build();
    public static final String TOKEN_ENDPOINT_PATH = "/oauth2/token";

    public void createCustomerEmployeeUser(String email) {
        logger.warn("Creating user with email: {} {}", email, iamServiceBaseUrl);
        var clientToken = getClientCredentialsJwt();

      Map<String, Object> createUserResponse = webClient.post()
            .uri(iamServiceBaseUrl + "/api/users")
            .header("Authorization", "Bearer " + clientToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                    {
                        "username": "%s",
                        "password": "{noop}password",
                        "roles": ["REALGUARDIO_CUSTOMER_EMPLOYEE"],
                        "enabled": true
                    }
                    """.formatted(email))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}).block();
      logger.info("Create user response: {}", createUserResponse);
    }

    public String getClientCredentialsJwt() {
      // Set the client ID and secret for UserDatabase profile
        String clientId = "realguardio-client";
        String clientSecret = "secret-rg";

        // Create Basic Auth header
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        // Prepare form data
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", "message.read message.write");

        // Send the POST request for client credentials

      Map<String, Object> tokenResponse = webClient.post()
          .uri(iamServiceBaseUrl + TOKEN_ENDPOINT_PATH)
          .header("Authorization", "Basic " + encodedAuth)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(BodyInserters.fromFormData(formData))
          .retrieve()
          .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}).block();

        if (tokenResponse == null) {
            throw new RuntimeException("Failed to get client credentials JWT");
        }
        
        String jwt = (String) tokenResponse.get("access_token");
        logger.info("Got client credentials JWT");
        return jwt;
    }

}