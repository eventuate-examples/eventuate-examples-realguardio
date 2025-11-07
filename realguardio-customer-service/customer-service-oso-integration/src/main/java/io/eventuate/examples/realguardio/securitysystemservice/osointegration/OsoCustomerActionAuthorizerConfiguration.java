package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerActionAuthorizer;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import io.realguardio.osointegration.ososervice.OsoServiceConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("UseOsoService")
@Import(OsoServiceConfiguration.class)
public class OsoCustomerActionAuthorizerConfiguration {

  @Bean
  public CustomerActionAuthorizer securitySystemActionAuthorizer(UserNameSupplier userNameSupplier,
                                                                 RealGuardOsoAuthorizer realGuardOsoAuthorizer) {
    return new OsoCustomerActionAuthorizer(userNameSupplier, realGuardOsoAuthorizer);
  }

}
