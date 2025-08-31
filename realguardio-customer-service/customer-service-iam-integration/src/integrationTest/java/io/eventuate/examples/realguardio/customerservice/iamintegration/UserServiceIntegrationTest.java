package io.eventuate.examples.realguardio.customerservice.iamintegration;

import io.eventuate.examples.realguardio.customerservice.security.UserService;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainer;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForLocalTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.lifecycle.Startables;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest(classes=UserServiceIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class UserServiceIntegrationTest {

  @Configuration
  @Import(IamIntegrationConfiguration.class)
  static class Config {
  }

  static AuthorizationServerContainerForLocalTests iamService;

  private static Integer iamServicePort;

  static {

    iamService = new AuthorizationServerContainerForLocalTests()
        .withUserDb()
        .withReuse(true)
    ;
    Startables.deepStart(iamService).join();

    iamServicePort = iamService.getMappedPort(AuthorizationServerContainer.PORT);
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {

    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> "http://localhost:" + iamService.getFirstMappedPort());
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://localhost:" + iamService.getFirstMappedPort() + "/oauth2/jwks");
  }

  @Autowired
  private UserService userService;

  @Test
  void shouldCreateUser() {
    String email = "test" + System.currentTimeMillis() + "@test.com";
    userService.createCustomerEmployeeUser(email);

    List<String> users = iamService.listUsers();

    assertThat(users).contains(email);
  }
}


