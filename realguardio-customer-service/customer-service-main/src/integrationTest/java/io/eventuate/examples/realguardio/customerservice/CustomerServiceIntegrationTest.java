package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.customermanagement.Customers;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerAndCustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerEmployee;
import io.eventuate.examples.realguardio.customerservice.testutils.Uniquifier;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainerForLocalTests;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
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
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerServiceIntegrationTest {

  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CustomerServiceIntegrationTest.class);

  @Autowired
  private TestRestTemplate restTemplate;


  public static EventuateKafkaNativeCluster eventuateKafkaCluster = new EventuateKafkaNativeCluster("customer-service-tests");

  public static EventuateKafkaNativeContainer kafka = eventuateKafkaCluster.kafka
      .withNetworkAliases("kafka")
      .withReuse(true)
      ;

  public static EventuateDatabaseContainer<?> database = DatabaseContainerFactory.makeVanillaDatabaseContainer()
      .withNetwork(eventuateKafkaCluster.network)
      .withNetworkAliases("database")
      .withReuse(true)
      ;

  static AuthorizationServerContainerForLocalTests     iamService = new AuthorizationServerContainerForLocalTests(Path.of("../../realguardio-iam-service/Dockerfile"))
      .withUserDb()
      .withReuse(true)
      .withNetworkAliases("database")
      .withLogConsumer(new Slf4jLogConsumer(logger).withPrefix("IAM"));



  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    Startables.deepStart(database, kafka, iamService).join();

    kafka.registerProperties(registry::add);
    database.registerProperties(registry::add);

    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> "http://localhost:" + iamService.getFirstMappedPort());
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> "http://localhost:" + iamService.getFirstMappedPort() + "/oauth2/jwks");
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
    String token = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort());
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
    EmailAddress adminUser = Uniquifier.uniquify(new EmailAddress("admin@example.com"));

    long customerId = createCustomer(adminUser);

    EmailAddress customerEmployee = Uniquifier.uniquify(new EmailAddress("john.doe@example.com"));

    createCustomerEmployee(adminUser, customerEmployee, customerId);


  }

  private void createCustomerEmployee(EmailAddress adminUser, EmailAddress customerEmployee, long customerId) {
    String token = JwtTokenHelper.getJwtTokenForUser(iamService.getFirstMappedPort(), null, adminUser.toString(), "password");
    HttpHeaders httpHeadersForAdmin = new HttpHeaders();
    httpHeadersForAdmin.set("Authorization", "Bearer " + token);
    httpHeadersForAdmin.setContentType(MediaType.APPLICATION_JSON);


    HttpEntity<String> entityForCreateEmployee = new HttpEntity<>("""
        {
            "personDetails": {
                "name": {
                    "firstName": "John",
                    "lastName": "Doe"
                },
                "emailAddress": {
                    "email": "%s"
                }
            }
        }
        """.formatted(customerEmployee), httpHeadersForAdmin);

    ResponseEntity<CustomerEmployee> responseForCreateEmployee = restTemplate.exchange(
        "/customers/%s/employees".formatted(customerId),
        HttpMethod.POST,
        entityForCreateEmployee,
        CustomerEmployee.class
    );

    assertThat(responseForCreateEmployee.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private long createCustomer(EmailAddress adminUser) {
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
                    "email": "%s"
                }
            }
        }
        """.formatted(adminUser), httpHeaders);

    ResponseEntity<CustomerAndCustomerEmployee> response = restTemplate.exchange(
        "/customers",
        HttpMethod.POST,
        entity,
        CustomerAndCustomerEmployee.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    long customerId = response.getBody().customer().getId();
    return customerId;
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
