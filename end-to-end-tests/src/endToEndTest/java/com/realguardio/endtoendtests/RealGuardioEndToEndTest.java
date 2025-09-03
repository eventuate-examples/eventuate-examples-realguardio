package com.realguardio.endtoendtests;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realguardio.endtoendtests.dto.CreateCustomerRequest;
import com.realguardio.endtoendtests.dto.CreateCustomerResponse;
import com.realguardio.endtoendtests.dto.EmailAddress;
import com.realguardio.endtoendtests.dto.PersonDetails;
import com.realguardio.endtoendtests.dto.PersonName;
import com.realguardio.endtoendtests.utils.JwtTokenHelper;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RealGuardioEndToEndTest {
    
    private static final Logger logger = LoggerFactory.getLogger(RealGuardioEndToEndTest.class);

    private static final ApplicationUnderTest aut = ApplicationUnderTest.make();

    private String authToken;
    
    @BeforeAll
    static void startContainers() {
        configureRestAssured();

        aut.start();

    }
    
    private static void configureRestAssured() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
    }
    
    @BeforeEach
    void setup() {
        authToken = JwtTokenHelper.getJwtTokenForUser(aut.getIamPort(), aut.iamServiceHostAndPort(), "user1", "password");
    }
    
    @Test 
    void dockerComposeShouldStart() {
        assertThat(aut.getCustomerServicePort()).isPositive();
        assertThat(aut.getOrchestrationServicePort()).isPositive();
        assertThat(aut.getSecurityServicePort()).isPositive();
        assertThat(aut.getIamPort()).isPositive();
    }
    
    @Test
    void shouldCreateCustomerAndSecuritySystem() {
        CreateCustomerResponse customerResponse = createCustomerResponse();
        long customerId = customerResponse.customer().id();

        assertThat(customerId).isGreaterThan(0);

        String locationName = "Office Main Entrance";

        // Start the saga via REST API
        String sagaRequestJson = """
		{
			"customerId": %d,
			"locationName": "%s"
		}
		""".formatted(customerId, locationName);

        logger.info("Starting CreateSecuritySystemSaga for customer {} and location {}", customerId, locationName);

        CreateSecuritySystemResponse createResponse =  RestAssured.given()
            .baseUri("http://localhost:" + aut.getOrchestrationServicePort())
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(sagaRequestJson)
            .log().all()
            .when()
            .post("/securitysystems")
            .then()
            .log().all()
            .statusCode(201) // Created - based on controller implementation
            .extract()
            .body()
            .as(CreateSecuritySystemResponse.class)
            ;

    }

    private CreateCustomerResponse createCustomerResponse() {
        // Arrange - Create unique test data
        String uniqueEmail = "admin-" + UUID.randomUUID() + "@example.com";


        // Act - Create customer
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
            "Test Customer",
            new PersonDetails(new PersonName("Admin", "User"), new EmailAddress(uniqueEmail)));

        CreateCustomerResponse customerResponse = RestAssured.given()
            .baseUri("http://localhost:" + aut.getCustomerServicePort())
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(customerRequest)
            .when()
            .post("/customers")
            .then()
            .statusCode(200)
            .extract()
            .as(CreateCustomerResponse.class);
        return customerResponse;
    }
}