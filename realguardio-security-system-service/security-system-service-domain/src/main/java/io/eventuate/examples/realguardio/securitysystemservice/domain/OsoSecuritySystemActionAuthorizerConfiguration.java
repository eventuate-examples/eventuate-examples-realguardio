package io.eventuate.examples.realguardio.securitysystemservice.domain;

import io.realguardio.osointegration.ososervice.OsoServiceConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("UseOsoService")
@Import(OsoServiceConfiguration.class)
public class OsoSecuritySystemActionAuthorizerConfiguration {

  @Bean
  public SecuritySystemActionAuthorizer securitySystemActionAuthorizer(UserNameSupplier userNameSupplier,
                                                                       RealGuardOsoAuthorizer realGuardOsoAuthorizer) {
    return new OsoSecuritySystemActionAuthorizer(userNameSupplier, realGuardOsoAuthorizer);
  }
}
