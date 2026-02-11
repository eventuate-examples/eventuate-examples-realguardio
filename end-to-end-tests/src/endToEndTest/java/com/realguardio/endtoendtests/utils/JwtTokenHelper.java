package com.realguardio.endtoendtests.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;

public class JwtTokenHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenHelper.class);
    private static final String CLIENT_CREDENTIALS = "realguardio-client:secret-rg";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Get JWT token for user with password grant
     */
    public static String getJwtTokenForUser(int iamServicePort) {
        logger.info("Requesting JWT token using password grant from IAM service at port {}", iamServicePort);
        
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(CLIENT_CREDENTIALS.getBytes());
        
        String accessToken = RestAssured.given()
            .baseUri("http://localhost:" + iamServicePort)
            .header("Authorization", authHeader)
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "password")
            .formParam("username", "user1")
            .formParam("password", "password")
            .when()
            .post("/oauth2/token")
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");
            
        logger.info("Successfully obtained JWT token");
        return accessToken;
    }
    
    /**
     * Get JWT token using client credentials grant
     */
    public static String getJwtToken(int iamServicePort) {
        logger.info("Requesting JWT token using client credentials from IAM service at port {}", iamServicePort);
        
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(CLIENT_CREDENTIALS.getBytes());
        
        String accessToken = RestAssured.given()
            .baseUri("http://localhost:" + iamServicePort)
            .header("Authorization", authHeader)
            .contentType(ContentType.URLENC)
            .formParam("grant_type", "client_credentials")
            .when()
            .post("/oauth2/token")
            .then()
            .statusCode(200)
            .extract()
            .path("access_token");
            
        logger.info("Successfully obtained JWT token");
        return accessToken;
    }

    public static String getJwtTokenForUserWithHostHeader(int iamServicePort) {
        return getJwtTokenForUser(iamServicePort, "iam-service:9000", "user1", "password");
    }

    /**
     * Get JWT token using password grant for specified username/password
     */
    public static String getJwtTokenForUser(int iamServicePort, String hostHeader, String username, String password) {
        logger.info("Requesting JWT token using password grant for user {} from IAM service at port {}", username, iamServicePort);

        WebClient.Builder webClientBuilder = WebClient.builder();
        WebClient webClient = webClientBuilder.build();

        var requestSpec = webClient.post()
            .uri("http://localhost:" + iamServicePort + "/oauth2/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString(CLIENT_CREDENTIALS.getBytes()));

        if (hostHeader != null) {
            requestSpec = requestSpec.header("Host", hostHeader);
        }

        String tokenResponseBody = requestSpec
            .bodyValue("grant_type=password&username=" + username + "&password=" + password)
            .retrieve()
            .bodyToMono(String.class)
            .block(TIMEOUT);

        logger.info("Token response: {}", tokenResponseBody);

        // Parse JSON response using ObjectMapper
        try {
            TokenResponse tokenResponse = objectMapper.readValue(tokenResponseBody, TokenResponse.class);
            return tokenResponse.accessToken();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }

}