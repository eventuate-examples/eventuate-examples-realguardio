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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public abstract class AbstractRealGuardioEndToEndTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractRealGuardioEndToEndTest.class);

    protected static ApplicationUnderTest aut = ApplicationUnderTest.make();


    private String authToken;
    

    protected static void configureRestAssured() {
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

        CustomerCreationResult customerResult = createCustomerWithAdmin();

        CreateCustomerResponse customerResponse = customerResult.response();
        long customerId = customerResponse.customer().id();
        long adminEmployeeId = customerResponse.initialAdministrator().id();
        String adminEmail = customerResult.adminEmail();

        String adminAuthToken = getTokenForCustomerAdmin(adminEmail);

        Long securitySystemId = createSecuritySystem(customerId);

        Long locationId = verifySecuritySystemHasLocationID(securitySystemId, adminAuthToken);

        assignPermissionsToLocation(adminEmployeeId, locationId, adminAuthToken, customerId);

        waitForUntilPermissionsHaveBeenAssigned(adminAuthToken, adminEmail, locationId);

        armSecuritySystem(securitySystemId, adminAuthToken);

        verifySecuritySystemArmed(adminAuthToken, securitySystemId);
    }

    private static String getTokenForCustomerAdmin(String adminEmail) {
        // Get JWT token for the customer's initial administrator that was created in IAM
        // The admin user has been created in IAM service during customer creation
        // We need to use their credentials for subsequent operations
        String adminAuthToken = JwtTokenHelper.getJwtTokenForUser(
            aut.getIamPort(),
            aut.iamServiceHostAndPort(),
            adminEmail,
            "password"  // Default password - in real scenario this would be set/changed
        );
        return adminAuthToken;
    }

    private @NotNull Long createSecuritySystem(long customerId) {
        String locationName = "Office Main Entrance";

        // Step 1: Use the Orchestration Service REST to create a security system (still using REALGUARDIO_ADMIN token)
        String sagaRequestJson = """
		{
			"customerId": %d,
			"locationName": "%s"
		}
		""".formatted(customerId, locationName);

        logger.info("Step 1: Starting CreateSecuritySystemSaga for customer {} and location {}", customerId, locationName);

        CreateSecuritySystemResponse createResponse = RestAssured.given()
            .baseUri("http://localhost:" + aut.getOrchestrationServicePort())
            .header("Authorization", "Bearer " + authToken)  // Using REALGUARDIO_ADMIN token for saga creation
            .contentType(ContentType.JSON)
            .body(sagaRequestJson)
            .log().all()
            .when()
            .post("/securitysystems")
            .then()
            .log().all()
            .statusCode(201)
            .extract()
            .body()
            .as(CreateSecuritySystemResponse.class);

        Long securitySystemId = createResponse.securitySystemId();
        assertThat(securitySystemId).isNotNull();
        logger.info("Security system created with ID: {}", securitySystemId);
        return securitySystemId;
    }

    private static Long verifySecuritySystemHasLocationID(Long securitySystemId, String adminAuthToken) {
        // Step 2: Use the Security Service API to verify that newly created SecuritySystem has been assigned a location ID
        logger.info("Step 2: Waiting for security system {} to be assigned a location ID", securitySystemId);

        Long locationId = await().atMost(60, TimeUnit.SECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> {
                var response = RestAssured.given()
                    .baseUri("http://localhost:" + aut.getSecurityServicePort())
                    .header("Authorization", "Bearer " + adminAuthToken)
                    .when()
                    .get("/securitysystems/" + securitySystemId)
                    .then()
                    .extract()
                    .response();
                
                if (response.statusCode() == 200) {
                    Long locId = response.jsonPath().getLong("locationId");
                    if (locId != null && locId > 0) {
                        logger.info("Security system {} has locationId: {}", securitySystemId, locId);
                        return locId;
                    }
                }
                logger.debug("Security system {} not yet assigned a location ID", securitySystemId);
                return null;
            }, loc -> loc != null && loc > 0);

        logger.info("Security system {} has been assigned location ID: {}", securitySystemId, locationId);
        return locationId;
    }

    private static void assignPermissionsToLocation(long adminEmployeeId, Long locationId, String adminAuthToken, long customerId) {
        // Step 3: Use the Customer Service REST API to assign the Customer's administrator arm/disarm rights to the location
        logger.info("Step 3: Assigning arm/disarm rights for employee {} at location {}", adminEmployeeId, locationId);

        // First assign CAN_ARM role
        String assignArmRoleJson = """
        {
            "employeeId": %d,
            "locationId": %d,
            "roleName": "CAN_ARM"
        }
        """.formatted(adminEmployeeId, locationId);

        RestAssured.given()
            .baseUri("http://localhost:" + aut.getCustomerServicePort())
            .header("Authorization", "Bearer " + adminAuthToken)  // Using admin's token
            .contentType(ContentType.JSON)
            .body(assignArmRoleJson)
            .log().all()
            .when()
            .put("/customers/" + customerId + "/location-roles")
            .then()
            .log().all()
            .statusCode(200);

        // Also assign CAN_DISARM role
        String assignDisarmRoleJson = """
        {
            "employeeId": %d,
            "locationId": %d,
            "roleName": "CAN_DISARM"
        }
        """.formatted(adminEmployeeId, locationId);

        RestAssured.given()
            .baseUri("http://localhost:" + aut.getCustomerServicePort())
            .header("Authorization", "Bearer " + adminAuthToken)  // Using admin's token
            .contentType(ContentType.JSON)
            .body(assignDisarmRoleJson)
            .log().all()
            .when()
            .put("/customers/" + customerId + "/location-roles")
            .then()
            .log().all()
            .statusCode(200);

        logger.info("Arm/disarm rights assigned successfully");
    }

    private static void armSecuritySystem(Long securitySystemId, String adminAuthToken) {
        // Step 4: Use the Security Service API to arm the security system
        logger.info("Step 4: Arming security system {}", securitySystemId);

        String armRequestJson = """
        {
            "action": "ARM"
        }
        """;

        RestAssured.given()
            .baseUri("http://localhost:" + aut.getSecurityServicePort())
            .header("Authorization", "Bearer " + adminAuthToken)
            .contentType(ContentType.JSON)
            .body(armRequestJson)
            .log().all()
            .when()
            .put("/securitysystems/" + securitySystemId)
            .then()
            .log().all()
            .statusCode(200);

        logger.info("Security system {} armed successfully", securitySystemId);
    }

    private static void verifySecuritySystemArmed(String adminAuthToken, Long securitySystemId) {
        String state = RestAssured.given()
            .baseUri("http://localhost:" + aut.getSecurityServicePort())
            .header("Authorization", "Bearer " + adminAuthToken)
            .when()
            .get("/securitysystems/" + securitySystemId)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("state");

        assertThat(state).isEqualTo("ARMED");
    }

    protected abstract void waitForUntilPermissionsHaveBeenAssigned(String adminAuthToken, String adminEmail, Long locationId);

    private record CustomerCreationResult(CreateCustomerResponse response, String adminEmail) {}
    
    private CustomerCreationResult createCustomerWithAdmin() {
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
        
        return new CustomerCreationResult(customerResponse, uniqueEmail);
    }
    
    private CreateCustomerResponse createCustomerResponse() {
        return createCustomerWithAdmin().response();
    }
}