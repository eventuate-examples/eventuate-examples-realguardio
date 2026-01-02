package io.realguardio.orchestration.sagas;

import io.eventuate.examples.realguardio.customerservice.api.messaging.commands.ValidateLocationCommand;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationNotFound;
import io.eventuate.examples.realguardio.customerservice.api.messaging.replies.LocationValidated;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemWithLocationIdCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationAlreadyHasSecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.eventuate.tram.sagas.testing.SagaUnitTestSupport.given;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateSecuritySystemWithLocationIdSagaTest {

    private SecuritySystemServiceProxy securitySystemServiceProxy;
    private CustomerServiceProxy customerServiceProxy;
    private PendingSecuritySystemResponses pendingResponses;

    private Long locationId = 100L;
    private Long customerId = 200L;
    private String locationName = "Warehouse";
    private Long securitySystemId = 300L;

    private CreateSecuritySystemWithLocationIdSaga makeCreateSecuritySystemWithLocationIdSaga() {
        return new CreateSecuritySystemWithLocationIdSaga(customerServiceProxy, securitySystemServiceProxy, pendingResponses);
    }

    @BeforeEach
    public void setUp() {
        securitySystemServiceProxy = new SecuritySystemServiceProxy();
        customerServiceProxy = new CustomerServiceProxy();
        pendingResponses = new PendingSecuritySystemResponses();
    }

    @Test
    public void shouldCreateSecuritySystemWithLocationIdSuccessfully() {
        CreateSecuritySystemWithLocationIdSagaData sagaData = new CreateSecuritySystemWithLocationIdSagaData(locationId);

        given()
            .saga(makeCreateSecuritySystemWithLocationIdSaga(), sagaData)
            .expect()
            .command(new ValidateLocationCommand(locationId))
            .to("customer-service")
            .andGiven()
            .successReply(new LocationValidated(locationId, locationName, customerId))
            .expect()
            .command(new CreateSecuritySystemWithLocationIdCommand(locationId, locationName))
            .to("security-system-service")
            .andGiven()
            .successReply(new SecuritySystemCreated(securitySystemId))
            .expectCompletedSuccessfully()
            .assertSagaData(data -> {
                assertThat(data.getSecuritySystemId()).isEqualTo(securitySystemId);
                assertThat(data.getLocationId()).isEqualTo(locationId);
                assertThat(data.getLocationName()).isEqualTo(locationName);
                assertThat(data.getCustomerId()).isEqualTo(customerId);
                assertThat(data.getRejectionReason()).isNull();
            });
    }

    @Test
    public void shouldRejectWhenLocationNotFound() {
        CreateSecuritySystemWithLocationIdSagaData sagaData = new CreateSecuritySystemWithLocationIdSagaData(locationId);

        given()
            .saga(makeCreateSecuritySystemWithLocationIdSaga(), sagaData)
            .expect()
            .command(new ValidateLocationCommand(locationId))
            .to("customer-service")
            .andGiven()
            .failureReply(new LocationNotFound())
            .expectRolledBack()
            .assertSagaData(data -> {
                assertThat(data.getRejectionReason()).isEqualTo("Location not found");
            });
    }

    @Test
    public void shouldRejectWhenLocationAlreadyHasSecuritySystem() {
        CreateSecuritySystemWithLocationIdSagaData sagaData = new CreateSecuritySystemWithLocationIdSagaData(locationId);

        given()
            .saga(makeCreateSecuritySystemWithLocationIdSaga(), sagaData)
            .expect()
            .command(new ValidateLocationCommand(locationId))
            .to("customer-service")
            .andGiven()
            .successReply(new LocationValidated(locationId, locationName, customerId))
            .expect()
            .command(new CreateSecuritySystemWithLocationIdCommand(locationId, locationName))
            .to("security-system-service")
            .andGiven()
            .failureReply(new LocationAlreadyHasSecuritySystem(locationId))
            .expectRolledBack()
            .assertSagaData(data -> {
                assertThat(data.getRejectionReason()).isEqualTo("Location already has a security system");
            });
    }
}
