package io.eventuate.examples.realguardio.securitysystemservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;

public class JwtTokenHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenHelper.class);
    private static final String CLIENT_CREDENTIALS = "realguardio-client:secret-rg";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Get JWT token for integration tests (no Host header needed)
     */
    public static String getJwtToken(int iamServicePort) {
        return getJwtToken(iamServicePort, null);
    }
    
    /**
     * Get JWT token for component tests (requires Host header)
     */
    public static String getJwtTokenWithHostHeader(int iamServicePort) {
        return getJwtToken(iamServicePort, "iam-service:9000");
    }
    
    /**
     * Get JWT token using password grant for user1/password
     */
    public static String getJwtTokenForUser(int iamServicePort) {
        return getJwtTokenForUser(iamServicePort, null, "user1", "password");
    }
    
    /**
     * Get JWT token using password grant with Host header for user1/password
     */
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
    
    private static String getJwtToken(int iamServicePort, String hostHeader) {
        logger.info("Requesting JWT token from IAM service at port {}", iamServicePort);
        
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
                .bodyValue("grant_type=client_credentials")
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