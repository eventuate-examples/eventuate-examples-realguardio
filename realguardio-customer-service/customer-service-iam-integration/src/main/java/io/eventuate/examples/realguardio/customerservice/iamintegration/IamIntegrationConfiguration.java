package io.eventuate.examples.realguardio.customerservice.iamintegration;

import io.eventuate.examples.realguardio.customerservice.security.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamIntegrationConfiguration {

  @Bean
  UserService userService() {
    return new UserServiceImpl();
  }
}
