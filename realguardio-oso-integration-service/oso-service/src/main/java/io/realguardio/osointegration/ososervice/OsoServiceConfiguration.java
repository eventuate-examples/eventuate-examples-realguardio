package io.realguardio.osointegration.ososervice;

import com.osohq.oso_cloud.Oso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;

@Configuration
@Import(RealGuardOsoAuthorizerConfiguration.class)
public class OsoServiceConfiguration {

  @Bean
  public Oso oso(@Value("${oso.url}") String osoUrl,
                 @Value("${oso.auth}") String osoAuth) {
    return new Oso(osoAuth, URI.create(osoUrl));
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
