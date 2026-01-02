package io.realguardio.osointegration.eventsubscribers;

import io.eventuate.tram.spring.testing.cloudcontract.EnableEventuateTramContractVerifier;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
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
        consumerName = "oso-integration-service")
@DirtiesContext
public class CustomerEventConsumerContractTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableEventuateTramContractVerifier
    @Import({OsoEventSubscribersConfiguration.class})
    public static class TestConfiguration {
    }

    @Autowired
    private StubFinder stubFinder;

    @MockitoBean
    private RealGuardOsoFactManager osoFactManager;

    private void eventuallyAssertThat(Runnable assertion) {
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(assertion::run);
    }

    @Test
    public void shouldHandleCustomerEmployeeAssignedCustomerRoleEvent() {
        stubFinder.trigger("customerEmployeeAssignedCustomerRoleEvent");
        eventuallyAssertThat(() ->
            verify(osoFactManager).createRoleInCustomer("owner@example.com", "123", "Owner")
        );
    }

    @Test
    public void shouldHandleCustomerEmployeeAssignedLocationRoleEvent() {
        stubFinder.trigger("customerEmployeeAssignedLocationRoleEvent");
        eventuallyAssertThat(() ->
            verify(osoFactManager).createRoleAtLocation("john.doe", "101", "Manager")
        );
    }

    @Test
    public void shouldHandleLocationCreatedForCustomerEvent() {
        stubFinder.trigger("locationCreatedForCustomerEvent");
        eventuallyAssertThat(() ->
            verify(osoFactManager).createLocationForCustomer("101", "123")
        );
    }

    @Test
    public void shouldHandleSecuritySystemAssignedToLocationEvent() {
        stubFinder.trigger("securitySystemAssignedToLocationEvent");
        eventuallyAssertThat(() ->
            verify(osoFactManager).assignSecuritySystemToLocation("401", "101")
        );
    }

}
