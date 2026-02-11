package io.eventuate.examples.realguardio.securitysystemservice.persistence.finder.oso;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemFinder;
import io.eventuate.examples.realguardio.securitysystemservice.osointegration.OsoSecuritySystemActionAuthorizerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("UseOsoService")
@Import(OsoSecuritySystemActionAuthorizerConfiguration.class)
public class SecuritySystemFinderUsingRepositoryWithOsoConfiguration {
    @Bean
    SecuritySystemFinder securitySystemFinder() {
        return new SecuritySystemFinderUsingRepositoryWithOso();
    }


    @Bean
    SecuritySystemRepositoryWithOsoImpl securitySystemRepositoryWithOso() {
        return new SecuritySystemRepositoryWithOsoImpl();
    }

}
