package io.eventuate.examples.realguardio.securitysystemservice.domain;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceClientImplTest {

    private WireMockServer wireMockServer;
    
    @Mock
    private JwtProvider jwtProvider;

    private CustomerServiceClientImpl customerServiceClient;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        
        restTemplate = new RestTemplate();
        String customerServiceUrl = "http://localhost:" + wireMockServer.port();
        customerServiceClient = new CustomerServiceClientImpl(restTemplate, customerServiceUrl, jwtProvider);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldReturnRolesWhenServiceReturnsSuccessfully() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        String jwtToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(jwtProvider.getCurrentJwtToken()).thenReturn(jwtToken);
        
        stubFor(get(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"roles\":[\"CAN_ARM\",\"CAN_DISARM\"]}")));

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).containsExactlyInAnyOrder("CAN_ARM", "CAN_DISARM");
        
        verify(getRequestedFor(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken)));
    }

    @Test
    void shouldReturnEmptySetWhenServiceReturns404() {
        // Given
        String userId = "123";
        Long locationId = 999L;
        String jwtToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(jwtProvider.getCurrentJwtToken()).thenReturn(jwtToken);
        
        stubFor(get(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken))
            .willReturn(aResponse()
                .withStatus(404)));

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
        
        verify(getRequestedFor(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken)));
    }

    @Test
    void shouldReturnEmptySetWhenServiceIsUnavailable() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        String jwtToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(jwtProvider.getCurrentJwtToken()).thenReturn(jwtToken);
        
        stubFor(get(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken))
            .willReturn(aResponse()
                .withStatus(503)));

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
        
        verify(getRequestedFor(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken)));
    }

    @Test
    void shouldReturnEmptySetWhenServiceReturns400() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        String jwtToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(jwtProvider.getCurrentJwtToken()).thenReturn(jwtToken);
        
        stubFor(get(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Bad Request\"}")));

        // When
        Set<String> result = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
        
        verify(getRequestedFor(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken)));
    }

    @Test
    void shouldHandleConnectionTimeout() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        String jwtToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(jwtProvider.getCurrentJwtToken()).thenReturn(jwtToken);
        
        // Configure RestTemplate with a short timeout for this test
        RestTemplate restTemplateWithTimeout = new RestTemplate();
        restTemplateWithTimeout.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(100);
            setReadTimeout(100);
        }});
        String customerServiceUrl = "http://localhost:" + wireMockServer.port();
        CustomerServiceClientImpl clientWithTimeout = new CustomerServiceClientImpl(
            restTemplateWithTimeout, customerServiceUrl, jwtProvider);
        
        stubFor(get(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken))
            .willReturn(aResponse()
                .withFixedDelay(1000)));  // 1 second delay

        // When
        Set<String> result = clientWithTimeout.getUserRolesAtLocation(userId, locationId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenNoJwtAvailable() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        
        when(jwtProvider.getCurrentJwtToken()).thenThrow(new IllegalStateException("No authentication found in security context"));

        // When/Then
        assertThatThrownBy(() -> customerServiceClient.getUserRolesAtLocation(userId, locationId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No authentication found in security context");
    }

    @Test
    void shouldSendCorrectRequestHeaders() {
        // Given
        String userId = "123";
        Long locationId = 456L;
        String jwtToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        when(jwtProvider.getCurrentJwtToken()).thenReturn(jwtToken);
        
        stubFor(get(urlEqualTo("/locations/" + locationId + "/roles"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"roles\":[\"CAN_VIEW\"]}")));

        // When
        customerServiceClient.getUserRolesAtLocation(userId, locationId);

        // Then
        verify(getRequestedFor(urlEqualTo("/locations/" + locationId + "/roles"))
            .withHeader("Authorization", equalTo(jwtToken)));
    }
}