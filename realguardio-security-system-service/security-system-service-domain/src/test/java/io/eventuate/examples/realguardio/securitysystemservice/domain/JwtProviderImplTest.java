package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtProviderImplTest {

    private final JwtProviderImpl jwtProvider = new JwtProviderImpl();

    @Test
    void shouldReturnJwtTokenWhenAuthenticationIsPresent() {
        // Given
        String tokenValue = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            
            // When
            String result = jwtProvider.getCurrentJwtToken();
            
            // Then
            assertThat(result).isEqualTo("Bearer " + tokenValue);
        }
    }

    @Test
    void shouldThrowExceptionWhenNoAuthenticationPresent() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        
        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            
            // When/Then
            assertThatThrownBy(() -> jwtProvider.getCurrentJwtToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authentication found in security context");
        }
    }

    @Test
    void shouldThrowExceptionWhenPrincipalIsNotJwt() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        
        when(authentication.getPrincipal()).thenReturn("not-a-jwt");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            
            // When/Then
            assertThatThrownBy(() -> jwtProvider.getCurrentJwtToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authentication principal is not a JWT token");
        }
    }
}