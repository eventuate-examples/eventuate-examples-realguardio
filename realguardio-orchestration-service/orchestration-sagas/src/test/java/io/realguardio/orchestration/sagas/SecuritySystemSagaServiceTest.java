package io.realguardio.orchestration.sagas;

import io.eventuate.tram.sagas.orchestration.SagaInstance;
import io.eventuate.tram.sagas.orchestration.SagaInstanceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecuritySystemSagaServiceTest {

    private SagaInstanceFactory sagaInstanceFactory;
    private CreateSecuritySystemSaga createSecuritySystemSaga;
    private SecuritySystemSagaService service;

    @BeforeEach
    void setUp() {
        sagaInstanceFactory = mock(SagaInstanceFactory.class);
        createSecuritySystemSaga = mock(CreateSecuritySystemSaga.class);
        service = new SecuritySystemSagaService(sagaInstanceFactory, createSecuritySystemSaga);
    }

    @Test
    void shouldReturnCompletableFutureWhenCreatingSecuritySystem() {
        Long customerId = 100L;
        String locationName = "Warehouse";
        
        SagaInstance sagaInstance = mock(SagaInstance.class);
        when(sagaInstance.getId()).thenReturn("saga-id");
        when(sagaInstanceFactory.create(eq(createSecuritySystemSaga), any(CreateSecuritySystemSagaData.class)))
                .thenReturn(sagaInstance);

        CompletableFuture<Long> future = service.createSecuritySystem(customerId, locationName);

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }
    
    @Test
    void shouldStartSagaAndStoreFuture() {
        Long customerId = 100L;
        String locationName = "Warehouse";
        String sagaId = "saga-123";
        
        SagaInstance sagaInstance = mock(SagaInstance.class);
        when(sagaInstance.getId()).thenReturn(sagaId);
        when(sagaInstanceFactory.create(eq(createSecuritySystemSaga), any(CreateSecuritySystemSagaData.class)))
                .thenReturn(sagaInstance);
        
        CompletableFuture<Long> future = service.createSecuritySystem(customerId, locationName);
        
        ArgumentCaptor<CreateSecuritySystemSagaData> dataCaptor = ArgumentCaptor.forClass(CreateSecuritySystemSagaData.class);
        verify(sagaInstanceFactory).create(eq(createSecuritySystemSaga), dataCaptor.capture());
        
        CreateSecuritySystemSagaData capturedData = dataCaptor.getValue();
        assertThat(capturedData.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedData.getLocationName()).isEqualTo(locationName);
        
        assertThat(service.hasPendingResponse(sagaId)).isTrue();
        assertThat(future).isNotNull();
    }
    
    @Test
    void shouldCompleteFutureWhenSecuritySystemCreated() {
        String sagaId = "saga-123";
        Long securitySystemId = 200L;
        
        SagaInstance sagaInstance = mock(SagaInstance.class);
        when(sagaInstance.getId()).thenReturn(sagaId);
        when(sagaInstanceFactory.create(eq(createSecuritySystemSaga), any(CreateSecuritySystemSagaData.class)))
                .thenReturn(sagaInstance);
        
        CompletableFuture<Long> future = service.createSecuritySystem(100L, "Warehouse");
        
        service.completeSecuritySystemCreation(sagaId, securitySystemId);
        
        assertThat(future.isDone()).isTrue();
        assertThat(future.getNow(null)).isEqualTo(securitySystemId);
        assertThat(service.hasPendingResponse(sagaId)).isFalse();
    }
}