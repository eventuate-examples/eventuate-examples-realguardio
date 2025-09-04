package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Component
public class CustomerServiceClientImpl implements CustomerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;

    public CustomerServiceClientImpl(RestTemplate restTemplate, 
                                     @Value("${customer.service.url:http://localhost:8081}") String customerServiceUrl) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
    }

    @Override
    public Set<String> getUserRolesAtLocation(String userId, Long locationId) {
        String url = customerServiceUrl + "/locations/" + locationId + "/roles";
        
        try {
            HttpHeaders headers = new HttpHeaders();
            
            // Get JWT token from Spring Security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
            }
            
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<RolesResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                RolesResponse.class
            );
            
            logger.info("Retrieved roles for user {} at location {}: {}", 
                userId, locationId, response.getBody().getRoles());
            
            return response.getBody().getRoles();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.info("No roles found for user {} at location {}", userId, locationId);
                return Set.of();
            }
            logger.error("HTTP error calling customer service: {}", e.getMessage());
            return Set.of();
        } catch (RestClientException e) {
            logger.error("Error calling customer service: {}", e.getMessage());
            return Set.of();
        }
    }
}