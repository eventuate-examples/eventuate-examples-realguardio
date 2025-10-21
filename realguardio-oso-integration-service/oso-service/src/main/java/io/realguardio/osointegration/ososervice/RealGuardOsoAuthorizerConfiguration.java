package io.realguardio.osointegration.ososervice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RealGuardOsoAuthorizerConfiguration {

    @Bean
    RealGuardOsoAuthorizer realGuardOsoAuthorizer(OsoService osoService) {
        return new RealGuardOsoAuthorizer(osoService);
    }

}
