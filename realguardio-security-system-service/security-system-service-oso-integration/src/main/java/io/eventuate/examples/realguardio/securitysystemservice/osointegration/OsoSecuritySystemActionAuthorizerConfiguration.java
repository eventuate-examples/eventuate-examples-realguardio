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
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("UseOsoService")
@Import(OsoServiceConfiguration.class)
public class OsoSecuritySystemActionAuthorizerConfiguration {

  @Bean
  @Profile("!OsoLocalSecuritySystemLocation")
  public SecuritySystemActionAuthorizer cloudSecuritySystemActionAuthorizer(UserNameSupplier userNameSupplier,
                                                                            RealGuardOsoAuthorizer realGuardOsoAuthorizer) {
    return new OsoSecuritySystemActionAuthorizer(userNameSupplier, realGuardOsoAuthorizer);
  }

  @Bean
  @Profile("OsoLocalSecuritySystemLocation")
  public SecuritySystemActionAuthorizer localSecuritySystemActionAuthorizer(UserNameSupplier userNameSupplier,
                                                                            RealGuardOsoAuthorizer realGuardOsoAuthorizer,
                                                                            JdbcTemplate jdbcTemplate) {
    return new OsoLocalSecuritySystemActionAuthorizer(userNameSupplier, realGuardOsoAuthorizer, jdbcTemplate);
  }

  @Bean
  @Profile("!OsoLocalSecuritySystemLocation")
  public LocalAuthorizationConfigFileSupplier defaultLocalAuthorizationConfigFileSupplier() {
      return new ClasspathLocalAuthorizationConfigFileSupplier();
  }

  @Bean
  @Profile("OsoLocalSecuritySystemLocation")
  public LocalAuthorizationConfigFileSupplier localSecuritySystemLocationConfigFileSupplier() {
      return new ClasspathLocalAuthorizationConfigFileSupplier(
              "/local_authorization_config_with_security_system_location.yaml");
  }
}
