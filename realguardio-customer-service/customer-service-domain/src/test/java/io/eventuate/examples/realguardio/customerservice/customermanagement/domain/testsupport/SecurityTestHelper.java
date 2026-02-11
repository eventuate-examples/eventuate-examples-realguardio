package io.eventuate.examples.realguardio.customerservice.customermanagement.domain.testsupport;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SecurityTestHelper {
    
    private final AuthenticationManager authenticationManager;
    
    public SecurityTestHelper(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public static <T> T withMockUser(MockUser user, Supplier<T> body) {
        try {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(user.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.username(), null, authorities));
            
            return body.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public static void withMockUser(MockUser user, Runnable body) {
        try {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(user.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.username(), null, authorities));
            
            body.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    public <T> T withRealUser(String email, String password, Supplier<T> body) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            return body.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    
    public void withRealUser(String email, String password, Runnable body) {
        withRealUser(email, password, () -> {
            body.run();
            return null;
        });
    }
}