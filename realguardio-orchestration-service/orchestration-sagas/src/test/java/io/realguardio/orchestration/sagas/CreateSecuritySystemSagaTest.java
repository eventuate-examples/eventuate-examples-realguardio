package io.realguardio.orchestration.sagas;

import io.realguardio.customer.api.*;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import io.realguardio.securitysystem.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.eventuate.tram.sagas.testing.SagaUnitTestSupport.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class CreateSecuritySystemSagaTest {

    private SecuritySystemServiceProxy securitySystemServiceProxy;
    private CustomerServiceProxy customerServiceProxy;
    private PendingSecuritySystemResponses pendingResponses;
    
    private Long customerId = 100L;
    private String locationName = "Warehouse";
    private Long securitySystemId = 200L;
    private Long locationId = 300L;

    private CreateSecuritySystemSaga makeCreateSecuritySystemSaga() {
        return new CreateSecuritySystemSaga(securitySystemServiceProxy, customerServiceProxy, pendingResponses);
    }

    @BeforeEach
    public void setUp() {
        securitySystemServiceProxy = new SecuritySystemServiceProxy();
        customerServiceProxy = new CustomerServiceProxy();
        pendingResponses = new PendingSecuritySystemResponses();
    }

    @Test
    public void shouldCreateSecuritySystemSuccessfully() {
        CreateSecuritySystemSagaData sagaData = new CreateSecuritySystemSagaData(customerId, locationName);
        
        given()
            .saga(makeCreateSecuritySystemSaga(), sagaData)
            .expect()
            .command(new CreateSecuritySystemCommand(locationName))
            .to("security-system-service")
            .andGiven()
            .successReply(new SecuritySystemCreated(securitySystemId))
            .expect()
            .command(new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId))
            .to("customer-service")
            .andGiven()
            .successReply(new LocationCreatedWithSecuritySystem(locationId))
            .expect()
            .command(new NoteLocationCreatedCommand(securitySystemId, locationId))
            .to("security-system-service")
            .andGiven()
            .successReply()
            .expectCompletedSuccessfully()
            .assertSagaData(data -> {
                assertEquals(securitySystemId, data.getSecuritySystemId());
                assertEquals(locationId, data.getLocationId());
                assertNull(data.getRejectionReason());
            });
    }

    @Test
    public void shouldRejectWhenCustomerNotFound() {
        CreateSecuritySystemSagaData sagaData = new CreateSecuritySystemSagaData(customerId, locationName);
        
        given()
            .saga(makeCreateSecuritySystemSaga(), sagaData)
            .expect()
            .command(new CreateSecuritySystemCommand(locationName))
            .to("security-system-service")
            .andGiven()
            .successReply(new SecuritySystemCreated(securitySystemId))
            .expect()
            .command(new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId))
            .to("customer-service")
            .andGiven()
            .failureReply(new CustomerNotFound())
            .expect()
            .command(new UpdateCreationFailedCommand(securitySystemId, "Customer not found"))
            .to("security-system-service")
            .andGiven()
            .successReply()
            .expectRolledBack()
            .assertSagaData(data -> {
                assertEquals(securitySystemId, data.getSecuritySystemId());
                assertEquals("Customer not found", data.getRejectionReason());
            });
    }

    @Test
    public void shouldRejectWhenLocationAlreadyHasSecuritySystem() {
        CreateSecuritySystemSagaData sagaData = new CreateSecuritySystemSagaData(customerId, locationName);
        
        given()
            .saga(makeCreateSecuritySystemSaga(), sagaData)
            .expect()
            .command(new CreateSecuritySystemCommand(locationName))
            .to("security-system-service")
            .andGiven()
            .successReply(new SecuritySystemCreated(securitySystemId))
            .expect()
            .command(new CreateLocationWithSecuritySystemCommand(customerId, locationName, securitySystemId))
            .to("customer-service")
            .andGiven()
            .failureReply(new LocationAlreadyHasSecuritySystem())
            .expect()
            .command(new UpdateCreationFailedCommand(securitySystemId, "Location already has security system"))
            .to("security-system-service")
            .andGiven()
            .successReply()
            .expectRolledBack()
            .assertSagaData(data -> {
                assertEquals(securitySystemId, data.getSecuritySystemId());
                assertEquals("Location already has security system", data.getRejectionReason());
            });
    }
}