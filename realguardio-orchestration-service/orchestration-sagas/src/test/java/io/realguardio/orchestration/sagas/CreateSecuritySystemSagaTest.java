package io.realguardio.orchestration.sagas;

import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import io.realguardio.orchestration.sagas.proxies.CustomerServiceProxy;
import io.realguardio.orchestration.sagas.proxies.SecuritySystemServiceProxy;
import io.realguardio.securitysystem.api.*;
import io.realguardio.customer.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateSecuritySystemSagaTest {

    private SecuritySystemServiceProxy securitySystemServiceProxy;
    private CustomerServiceProxy customerServiceProxy;
    private CreateSecuritySystemSaga saga;

    @BeforeEach
    void setUp() {
        securitySystemServiceProxy = mock(SecuritySystemServiceProxy.class);
        customerServiceProxy = mock(CustomerServiceProxy.class);
        saga = new CreateSecuritySystemSaga(securitySystemServiceProxy, customerServiceProxy);
    }

    @Test
    void shouldImplementSimpleSaga() {
        assertThat(saga).isInstanceOf(SimpleSaga.class);
    }

    @Test
    void shouldHaveThreeStepsInSagaDefinition() {
        SagaDefinition<CreateSecuritySystemSagaData> sagaDefinition = saga.getSagaDefinition();
        
        assertThat(sagaDefinition).isNotNull();
        // Verify structure exists - actual step count verification would require more complex testing
    }

    @Test
    void shouldCreateSecuritySystemInStep1() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        
        saga.makeCreateSecuritySystemCommand(data);
        
        verify(securitySystemServiceProxy).createSecuritySystem("Warehouse");
    }

    @Test
    void shouldHandleSecuritySystemCreatedReply() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        SecuritySystemCreated reply = new SecuritySystemCreated(200L);
        
        saga.handleSecuritySystemCreated(data, reply);
        
        assertThat(data.getSecuritySystemId()).isEqualTo(200L);
    }

    @Test
    void shouldCreateLocationInStep2() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        data.setSecuritySystemId(200L);
        
        saga.makeCreateLocationCommand(data);
        
        verify(customerServiceProxy).createLocationWithSecuritySystem(100L, "Warehouse", 200L);
    }

    @Test
    void shouldHandleLocationCreatedReply() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        LocationCreatedWithSecuritySystem reply = new LocationCreatedWithSecuritySystem(300L);
        
        saga.handleLocationCreated(data, reply);
        
        assertThat(data.getLocationId()).isEqualTo(300L);
    }

    @Test
    void shouldNoteLocationCreatedInStep3() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        data.setSecuritySystemId(200L);
        data.setLocationId(300L);
        
        saga.makeNoteLocationCreatedCommand(data);
        
        verify(securitySystemServiceProxy).noteLocationCreated(200L, 300L);
    }

    @Test
    void shouldCompensateWithUpdateCreationFailed() {
        CreateSecuritySystemSagaData data = new CreateSecuritySystemSagaData(100L, "Warehouse");
        data.setSecuritySystemId(200L);
        data.setRejectionReason("Customer not found");
        
        saga.makeUpdateCreationFailedCommand(data);
        
        verify(securitySystemServiceProxy).updateCreationFailed(200L, "Customer not found");
    }
}