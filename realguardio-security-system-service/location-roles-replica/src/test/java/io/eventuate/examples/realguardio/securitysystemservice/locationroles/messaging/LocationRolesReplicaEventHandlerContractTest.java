package io.eventuate.examples.realguardio.securitysystemservice.locationroles.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.locationroles.common.LocationRolesReplicaService;
import io.eventuate.tram.spring.testing.cloudcontract.EnableEventuateTramContractVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(ids = "io.eventuate.examples.realguardio:customer-service-event-publishing",
        stubsMode = StubRunnerProperties.StubsMode.CLASSPATH,
        stubsPerConsumer = true,
        consumerName = "security-system-service")
@DirtiesContext
public class LocationRolesReplicaEventHandlerContractTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableEventuateTramContractVerifier
    @Import({LocationRolesReplicaMessagingConfiguration.class})
    public static class TestConfiguration {
    }

    @Autowired
    private StubFinder stubFinder;

    @MockitoBean
    private LocationRolesReplicaService locationRolesReplicaService;

    @Test
    public void shouldHandleLocationCreatedForCustomerEvent() {
        stubFinder.trigger("locationCreatedForCustomerEvent");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(locationRolesReplicaService).saveLocation(101L, "123");
        });
    }

    @Test
    public void shouldHandleCustomerEmployeeAssignedLocationRoleEvent() {
        stubFinder.trigger("customerEmployeeAssignedLocationRoleEvent");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(locationRolesReplicaService).saveLocationRole("john.doe", 101L, "Manager");
        });
    }

    @Test
    public void shouldHandleTeamMemberAddedEvent() {
        stubFinder.trigger("teamMemberAddedEvent");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(locationRolesReplicaService).saveTeamMember("201", "301");
        });
    }

    @Test
    public void shouldHandleTeamAssignedLocationRoleEvent() {
        stubFinder.trigger("teamAssignedLocationRoleEvent");
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(locationRolesReplicaService).saveTeamLocationRole("201", "Admin", 101L);
        });
    }

}
