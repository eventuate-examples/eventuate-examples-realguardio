package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtProviderImpl implements JwtProvider {

    @Override
    public String getCurrentJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new IllegalStateException("No authentication found in security context");
        }
        
        if (!(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalStateException("Authentication principal is not a JWT token");
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return "Bearer " + jwt.getTokenValue();
    }
}