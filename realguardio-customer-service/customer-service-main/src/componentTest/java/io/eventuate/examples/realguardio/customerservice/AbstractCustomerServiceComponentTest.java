package io.eventuate.examples.realguardio.customerservice;

import io.eventuate.common.testcontainers.DatabaseContainerFactory;
import io.eventuate.common.testcontainers.EventuateDatabaseContainer;
import io.eventuate.examples.realguardio.customerservice.commondomain.EmailAddress;
import io.eventuate.examples.realguardio.customerservice.restapi.RolesResponse;
import io.eventuate.examples.springauthorizationserver.testcontainers.AuthorizationServerContainer;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeCluster;
import io.eventuate.messaging.kafka.testcontainers.EventuateKafkaNativeContainer;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupport;
import io.eventuate.tram.spring.testing.outbox.commands.CommandOutboxTestSupportConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

public abstract class AbstractCustomerServiceComponentTest {

  protected static Logger logger = LoggerFactory.getLogger(CustomerServiceComponentTest.class);
  protected String baseUri;

  @Configuration
  @EnableAutoConfiguration
  @Import({
      CommandOutboxTestSupportConfiguration.class,
      ComponentTestSupportConfiguration.class
  })
  static class AbstractConfig {
  }

  public static EventuateKafkaNativeCluster eventuateKafkaCluster;


  public static EventuateKafkaNativeContainer kafka;


  public static EventuateDatabaseContainer<?> database;


  protected static void makeContainers(String networkSuffix) {
      eventuateKafkaCluster = new EventuateKafkaNativeCluster("customer-service-tests-" + networkSuffix);
      kafka = eventuateKafkaCluster.kafka
          .withNetworkAliases("kafka")
          .withReuse(true);
      database = DatabaseContainerFactory.makeVanillaDatabaseContainer()
          .withNetwork(eventuateKafkaCluster.network)
          .withNetworkAliases("database")
          .withReuse(true);
  }

  public static AuthorizationServerContainer iamService;


  @Autowired
  protected CommandOutboxTestSupport commandOutboxTestSupport;

  @Autowired
  protected ComponentTestSupport componentTestSupport;


  protected RolesResponse getRolesForLocation(String realGuardIOAdminAccessToken, Long locationId) {
    logger.info("Getting roles for locationId: {}", locationId);
    return RestAssured.given()
        .baseUri(baseUri)
        .header("Authorization", "Bearer " + realGuardIOAdminAccessToken)
        .when()
        .get("/locations/" + locationId + "/roles")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .as(RolesResponse.class);
  }

  protected Long createLocation(long customerId, String accessToken) {
    logger.info("Creating location for customerId: {}", customerId);

    String locationName = "Office Front Door";
    String requestBody = """
        {"name": "%s"}
        """.formatted(locationName);

    var response = RestAssured.given()
        .baseUri(baseUri)
        .header("Authorization", "Bearer " + accessToken)
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/customers/" + customerId + "/locations")
        .then()
        .statusCode(200)
        .extract()
        .body();

    Integer locationIdAsInteger = response.path("locationId");
    return Long.valueOf(locationIdAsInteger);
  }

  protected CustomerSummary createCustomer(EmailAddress adminUser, String realGuardIOAdminAccessToken) {

    logger.info("Creating customer with admin user {}", adminUser);
    // Create customer
    String customerJson = """
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
""".formatted(adminUser);

    var response = RestAssured.given()
        .baseUri(baseUri)
        .header("Authorization", "Bearer " + realGuardIOAdminAccessToken)
        .contentType(ContentType.JSON)
        .body(customerJson)
        .when()
        .post("/customers")
        .then()
        .statusCode(200)
        .extract()
        .body();
    
    Integer customerIdAsInteger = response.path("customer.id");
    long customerId = Long.valueOf(customerIdAsInteger);
    
    Integer employeeIdAsInteger = response.path("initialAdministrator.id");
    long employeeId = Long.valueOf(employeeIdAsInteger);
    
    return new CustomerSummary(customerId, employeeId);
  }

  protected String getAccessTokenForRealGuardIoAdmin() {
    return JwtTokenHelper.getJwtTokenForUserWithHostHeader(iamService.getFirstMappedPort());
  }


}
