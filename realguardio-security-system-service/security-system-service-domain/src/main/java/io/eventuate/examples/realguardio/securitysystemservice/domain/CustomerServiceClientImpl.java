package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Component
public class CustomerServiceClientImpl implements CustomerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;
    private final JwtProvider jwtProvider;

    public CustomerServiceClientImpl(RestTemplate restTemplate, 
                                     @Value("${customer.service.url:http://localhost:8081}") String customerServiceUrl,
                                     JwtProvider jwtProvider) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Set<String> getUserRolesAtLocation(String userId, Long locationId) {
        String url = customerServiceUrl + "/locations/" + locationId + "/roles";

        HttpHeaders headers = new HttpHeaders();

        // Get JWT token from JwtProvider
        String jwtToken = jwtProvider.getCurrentJwtToken();
        headers.set(HttpHeaders.AUTHORIZATION, jwtToken);

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
    }
}