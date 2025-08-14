package io.eventuate.examples.realguardio.customerservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public UserDetailsManager userDetailsManager(PasswordEncoder passwordEncoder) {
        // Create initial admin user
        UserDetails admin = User.builder()
            .username("admin@realguard.io")
            .password(passwordEncoder.encode("admin123"))
            .roles("REALGUARDIO_ADMIN")
            .build();
            
        return new InMemoryUserDetailsManager(admin);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsManager userDetailsManager,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsManager);
        authProvider.setPasswordEncoder(passwordEncoder);
        
        return new ProviderManager(authProvider);
    }
    
    @Bean
    public UserNameSupplier userNameSupplier() {
        return new UserNameSupplier();
    }

    @Bean
    UserService userService(UserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        return new UserService(userDetailsManager, passwordEncoder);
    }
    
}