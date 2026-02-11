package io.eventuate.examples.realguardio.securitysystemservice.persistence.finder.repository;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!UseOsoService")
public class SecuritySystemFinderUsingRepositoryConfiguration {
    @Bean
    SecuritySystemFinder securitySystemFinder() {
        return new SecuritySystemFinderUsingRepository();
    }
}
