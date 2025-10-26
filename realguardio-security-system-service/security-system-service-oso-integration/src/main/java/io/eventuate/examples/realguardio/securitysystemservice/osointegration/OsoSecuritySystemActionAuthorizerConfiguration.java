package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemActionAuthorizer;
import io.eventuate.examples.realguardio.securitysystemservice.domain.UserNameSupplier;
import io.realguardio.osointegration.ososervice.LocalAuthorizationConfigFileSupplier;
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

  @Bean
  public LocalAuthorizationConfigFileSupplier localAuthorizationConfigFileSupplier() {
      return new ClasspathLocalAuthorizationConfigFileSupplier();
  }
}
