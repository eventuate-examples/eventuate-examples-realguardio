package io.realguardio.osointegration.ososervice;

import com.osohq.oso_cloud.Oso;
import com.osohq.oso_cloud.OsoClientOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;

@Configuration
@Import(RealGuardOsoAuthorizerConfiguration.class)
public class OsoServiceConfiguration {

  @Autowired(required = false)
  private LocalAuthorizationConfigFileSupplier localAuthorizationConfigFileSupplier;

  @Bean
  public Oso oso(@Value("${oso.url}") String osoUrl,
                 @Value("${oso.auth}") String osoAuth) {
      var optionsBuilder = new OsoClientOptions.Builder();
      if (localAuthorizationConfigFileSupplier != null) {
          optionsBuilder.withDataBindingsPath(localAuthorizationConfigFileSupplier.get());
      }
      return new Oso(osoAuth, URI.create(osoUrl), optionsBuilder.build());
  }

  @Bean
  OsoService osoService(Oso oso) {
    return new OsoService(oso);
  }

  @Bean
  RealGuardOsoFactManager realGuardOsoFactManager(OsoService osoService) {
    return new RealGuardOsoFactManager(osoService);
  }

}
