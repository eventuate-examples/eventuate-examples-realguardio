package io.realguardio.osointegration.ososervice;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = RealGuardOsoAuthorizerTest.TestConfig.class)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.osoAuthorizer.failure-rate-threshold=100",
    "resilience4j.circuitbreaker.instances.osoAuthorizer.sliding-window-type=COUNT_BASED",
    "resilience4j.circuitbreaker.instances.osoAuthorizer.slidingWindowSize=3",
    "resilience4j.circuitbreaker.instances.osoAuthorizer.minimum-number-of-calls=3",
    "resilience4j.circuitbreaker.instances.osoAuthorizer.wait-duration-in-open-state=10s",
    "resilience4j.circuitbreaker.instances.osoAuthorizer.wait-duration-in-open-state=10s",
    "resilience4j.circuitbreaker.instances.osoAuthorizer.permitted-number-of-calls-in-half-open-state=2",
    "resilience4j.retry.instances.osoAuthorizer.max-attempts=3",
    "resilience4j.retry.instances.osoAuthorizer.wait-duration=100ms",
    "resilience4j.retry.instances.osoAuthorizer.retry-exceptions=java.lang.RuntimeException,java.util.concurrent.TimeoutException",
    "resilience4j.retry.instances.osoAuthorizer.ignore-exceptions=io.github.resilience4j.circuitbreaker.CallNotPermittedException",
    "resilience4j.timelimiter.instances.osoAuthorizer.timeout-duration=2s",
    "resilience4j.timelimiter.instances.osoAuthorizer.cancel-running-future=true"
})
class RealGuardOsoAuthorizerTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    @Import(RealGuardOsoAuthorizerConfiguration.class)
    static class TestConfig {
    }

    @Autowired
    private RealGuardOsoAuthorizer authorizer;

    @MockBean
    private OsoService mockOsoService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        // Reset circuit breaker state before each test
        circuitBreakerRegistry.circuitBreaker("osoAuthorizer").reset();
    }

    @AfterEach
    void tearDown() {
        reset(mockOsoService);
    }

    @Test
    void shouldReturnTrueWhenUserIsAuthorized() {
        // Given
        when(mockOsoService.authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "123"))
                .thenReturn(true);

        // When
        boolean result = isAuthorized("alice", "arm", "123");

        // Then
        assertThat(result).isTrue();
        verify(mockOsoService).authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "123");
    }

    @Test
    void shouldReturnFalseWhenUserIsNotAuthorized() {
        // Given
        when(mockOsoService.authorize("CustomerEmployee", "bob", "disarm", "SecuritySystem", "456"))
                .thenReturn(false);

        // When
        boolean result = isAuthorized("bob", "disarm", "456");

        // Then
        assertThat(result).isFalse();
        verify(mockOsoService).authorize("CustomerEmployee", "bob", "disarm", "SecuritySystem", "456");
    }

    @Test
    void shouldRetryOnTransientIOException() {
        // Given - First two calls fail with IOException, third succeeds
        when(mockOsoService.authorize(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException(new IOException("Connection reset")))
                .thenThrow(new RuntimeException(new IOException("Connection timeout")))
                .thenReturn(true);

        // When
        boolean result = isAuthorized("alice", "arm", "123");

        // Then - Should succeed after retries
        assertThat(result).isTrue();
        // Verify it was called 3 times (initial + 2 retries)
        verify(mockOsoService, times(3)).authorize(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldRetryOnTimeoutException() {
        // Given - First two calls timeout, third succeeds
        when(mockOsoService.authorize(anyString(), anyString(), anyString(), anyString(), anyString()))
                .then((Answer<Boolean>) invocation -> {
                    TimeUnit.SECONDS.sleep(5);
                    return Boolean.FALSE;
                })
                .then((Answer<Boolean>) invocation -> {
                    TimeUnit.SECONDS.sleep(5);
                    return Boolean.FALSE;
                })
                .thenReturn(true);

        // When
        boolean result = isAuthorized("bob", "disarm", "789");

        // Then - Should succeed after retries
        assertThat(result).isTrue();
        // Verify it was called 3 times (initial + 2 retries)
        verify(mockOsoService, times(3)).authorize(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldReturnFalseFromFallbackAfterMaxRetryAttemptsExceeded() {
        // Given - All calls fail with IOException
        when(mockOsoService.authorize(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException(new IOException("Connection reset")));

        // When
        boolean result = isAuthorized("alice", "arm", "123");

        // Then - Should return false from fallback after all retries
        assertThat(result).isFalse();
        // Verify it was called 3 times (initial + 2 retries)
        verify(mockOsoService, times(3)).authorize(anyString(), anyString(), anyString(), anyString(), anyString());
    }


    @Test
    void shouldUseFallbackWhenCircuitBreakerIsOpen() {
        // Given - Open the circuit by causing failures
        when(mockOsoService.authorize(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException(new IOException("Service down")));

        // Open the circuit
        isAuthorized("alice", "arm", "1");
        isAuthorized("bob", "arm", "2");

        // When - Circuit is now open, reset mock but it shouldn't be called
        reset(mockOsoService);
        when(mockOsoService.authorize(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // Then - Fallback should be used, returning false to deny access
        boolean result = isAuthorized("charlie", "disarm", "999");
        assertThat(result).isFalse();

        // Verify the mock was not called (circuit breaker prevented the call)
        verify(mockOsoService, never()).authorize(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void shouldHandleDifferentActionsCorrectly() {
        // Given
        when(mockOsoService.authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "100"))
                .thenReturn(true);
        when(mockOsoService.authorize("CustomerEmployee", "alice", "disarm", "SecuritySystem", "100"))
                .thenReturn(false);

        // When / Then
        assertThat(isAuthorized("alice", "arm", "100")).isTrue();
        assertThat(isAuthorized("alice", "disarm", "100")).isFalse();

        verify(mockOsoService).authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "100");
        verify(mockOsoService).authorize("CustomerEmployee", "alice", "disarm", "SecuritySystem", "100");
    }

    @Test
    void shouldHandleDifferentUsersCorrectly() {
        // Given
        when(mockOsoService.authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "100"))
                .thenReturn(true);
        when(mockOsoService.authorize("CustomerEmployee", "bob", "arm", "SecuritySystem", "100"))
                .thenReturn(false);

        // When / Then
        assertThat(isAuthorized("alice", "arm", "100")).isTrue();
        assertThat(isAuthorized("bob", "arm", "100")).isFalse();

        verify(mockOsoService).authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "100");
        verify(mockOsoService).authorize("CustomerEmployee", "bob", "arm", "SecuritySystem", "100");
    }

    @Test
    void shouldHandleDifferentSecuritySystemsCorrectly() {
        // Given
        when(mockOsoService.authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "100"))
                .thenReturn(true);
        when(mockOsoService.authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "200"))
                .thenReturn(false);

        // When / Then
        assertThat(isAuthorized("alice", "arm", "100")).isTrue();
        assertThat(isAuthorized("alice", "arm", "200")).isFalse();

        verify(mockOsoService).authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "100");
        verify(mockOsoService).authorize("CustomerEmployee", "alice", "arm", "SecuritySystem", "200");
    }

    private boolean isAuthorized(String user, String action, String securitySystem) {
        try {
            return authorizer.isAuthorized(user, action, securitySystem).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
