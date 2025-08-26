package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.examples.realguardio.customerservice.customermanagement.Customers;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForLocalTests;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerServiceIntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;


  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
      .withDatabaseName("testdb")
      .withUsername("testuser")
      .withPassword("testpass")
      .withReuse(true);

  static AuthorizationServerContainerForLocalTests iamService;

  static {

    iamService = new AuthorizationServerContainerForLocalTests()
        .withUserDb()
        .withReuse(true)
    ;
    Startables.deepStart(iamService, postgres).join();
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> "http://localhost:" + iamService.getMappedPort(9000));
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://localhost:" + iamService.getMappedPort(9000) + "/oauth2/jwks");
  }

  @Test
  void shouldReturnUnauthorizedWithoutToken() {
    ResponseEntity<String> response = restTemplate.exchange(
        "/customers",
        HttpMethod.GET,
        null,
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void shouldReturnCustomersWithValidToken() {
    HttpHeaders headers = makeHeadersWithJwt();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<Customers> response = restTemplate.exchange(
        "/customers",
        HttpMethod.GET,
        entity,
        Customers.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Customers customers = response.getBody();
    assertThat(customers).isNotNull();
    assertThat(customers.customers()).isNotNull();
    assertThat(customers.customers()).hasSizeGreaterThan(0);
  }

  private static @NotNull HttpHeaders makeHeadersWithJwt() {
    String token = JwtTokenHelper.getJwtTokenForUser(iamService.getMappedPort(9000));
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    return headers;
  }

  @Test
  public void anonymousRequestShouldNotCreateCustomer() {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>("""
        {
            "name": "New Customer",
            "initialAdministrator": {
                "name": {
                    "firstName": "Admin",
                    "lastName": "User"
                },
                "emailAddress": {
                    "email": "admin@example.com"
                }
            }
        }
        """, httpHeaders);
    ResponseEntity<Customers> response = restTemplate.exchange(
        "/customers",
        HttpMethod.POST,
        entity,
        Customers.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

  }

  @Test
  public void shouldCreateCustomer() {
    HttpHeaders httpHeaders = makeHeadersWithJwt();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>("""
        {
            "name": "New Customer",
            "initialAdministrator": {
                "name": {
                    "firstName": "Admin",
                    "lastName": "User"
                },
                "emailAddress": {
                    "email": "admin@example.com"
                }
            }
        }
        """, httpHeaders);
    ResponseEntity<Customers> response = restTemplate.exchange(
        "/customers",
        HttpMethod.POST,
        entity,
        Customers.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

  }


        /*
        // Verify the data
        var systems = response.getBody().customers();
        assertThat(systems).extracting(Customer::getLocationName)
                .containsExactlyInAnyOrder(DBInitializer.LOCATION_OAKLAND_OFFICE, 
                        DBInitializer.LOCATION_BERKELEY_OFFICE, 
                        DBInitializer.LOCATION_HAYWARD_OFFICE);
        
        // Verify specific system details
        var oaklandOffice = systems.stream()
                .filter(s -> s.getLocationName().equals(DBInitializer.LOCATION_OAKLAND_OFFICE))
                .findFirst()
                .orElseThrow();
        assertThat(oaklandOffice.getState()).isEqualTo(CustomerState.ARMED);
        assertThat(oaklandOffice.getActions()).containsExactly(CustomerAction.DISARM);
        
        var haywardOffice = systems.stream()
                .filter(s -> s.getLocationName().equals(DBInitializer.LOCATION_HAYWARD_OFFICE))
                .findFirst()
                .orElseThrow();
        assertThat(haywardOffice.getState()).isEqualTo(CustomerState.ALARMED);
        assertThat(haywardOffice.getActions()).containsExactlyInAnyOrder(
                CustomerAction.ACKNOWLEDGE, CustomerAction.DISARM);

         */
}
