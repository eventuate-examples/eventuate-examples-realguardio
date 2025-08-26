package io.eventuate.examples.realguardio.customerservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    

    @Bean
    public UserNameSupplier userNameSupplier() {
        return new UserNameSupplier();
    }
}