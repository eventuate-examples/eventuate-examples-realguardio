package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private CustomerServiceClientImpl customerServiceClient;

    private final String customerServiceUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        customerServiceClient = new CustomerServiceClientImpl(restTemplate, customerServiceUrl);
    }

    @Test
    void shouldReturnRolesWhenServiceReturnsSuccessfully() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        RolesResponse rolesResponse = new RolesResponse(Set.of("CAN_ARM", "CAN_DISARM"));
        ResponseEntity<RolesResponse> responseEntity = ResponseEntity.ok(rolesResponse);

        when(restTemplate.exchange(
            eq(customerServiceUrl + "/locations/" + locationId + "/roles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(RolesResponse.class)
        )).thenReturn(responseEntity);

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM");
    }

    @Test
    void shouldReturnEmptySetWhenServiceReturns404() {
        // Given
        String userId = "123";
        Long locationId = 999L;

        when(restTemplate.exchange(
            eq(customerServiceUrl + "/locations/" + locationId + "/roles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(RolesResponse.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptySetWhenServiceIsUnavailable() {
        // Given
        String userId = "123";
        Long locationId = 456L;

        when(restTemplate.exchange(
            eq(customerServiceUrl + "/locations/" + locationId + "/roles"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(RolesResponse.class)
        )).thenThrow(new RestClientException("Service unavailable"));

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldForwardJwtTokenFromSecurityContext() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        String tokenValue = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        RolesResponse rolesResponse = new RolesResponse(Set.of("CAN_ARM"));
        ResponseEntity<RolesResponse> responseEntity = ResponseEntity.ok(rolesResponse);

        // Mock SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            
            when(restTemplate.exchange(
                eq(customerServiceUrl + "/locations/" + locationId + "/roles"),
                eq(HttpMethod.GET),
                argThat(entity -> {
                    HttpHeaders headers = ((HttpEntity<?>) entity).getHeaders();
                    return ("Bearer " + tokenValue).equals(headers.getFirst(HttpHeaders.AUTHORIZATION));
                }),
                eq(RolesResponse.class)
            )).thenReturn(responseEntity);

            // When
            Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

            // Then
            assertThat(result).containsExactlyInAnyOrder("CAN_ARM");
        }
    }

    @Test
    void shouldHandleNoAuthenticationInSecurityContext() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        RolesResponse rolesResponse = new RolesResponse(Set.of("CAN_ARM"));
        ResponseEntity<RolesResponse> responseEntity = ResponseEntity.ok(rolesResponse);

        // Mock SecurityContext with no authentication
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        
        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            
            when(restTemplate.exchange(
                eq(customerServiceUrl + "/locations/" + locationId + "/roles"),
                eq(HttpMethod.GET),
                argThat(entity -> {
                    HttpHeaders headers = ((HttpEntity<?>) entity).getHeaders();
                    return headers.getFirst(HttpHeaders.AUTHORIZATION) == null;
                }),
                eq(RolesResponse.class)
            )).thenReturn(responseEntity);

            // When
            Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

            // Then
            assertThat(result).containsExactlyInAnyOrder("CAN_ARM");
        }
    }
}