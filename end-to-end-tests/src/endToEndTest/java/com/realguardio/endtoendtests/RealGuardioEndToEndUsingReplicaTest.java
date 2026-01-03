package com.realguardio.endtoendtests;

import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;

import static org.assertj.core.api.Assertions.assertThat;

public class RealGuardioEndToEndUsingReplicaTest extends AbstractRealGuardioEndToEndTest{

    @BeforeAll
    static void startContainers() {
        aut = makeAut("e2e-replica");
        aut.useLocationRolesReplica();
        configureRestAssured();
        aut.start();
    }

    @Override
    protected void waitForUntilPermissionsHaveBeenAssigned(String adminAuthToken, String adminEmail, Long locationId) {
        logger.info("Waiting for event to be consumed and processed by the service");
        Awaitility.await().untilAsserted(() -> {

            var response = RestAssured.given()
                .baseUri(String.format("http://localhost:%d", aut.getSecurityServicePort()))
                .header("Authorization", "Bearer " + adminAuthToken)
                .queryParam("userName", adminEmail)
                .queryParam("locationId", locationId)
                .when()
                .get("/location-roles")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

            assertThat(response.getList("$")).isNotEmpty();

            logger.info("Location role successfully retrieved via REST API");
        });
    }
}