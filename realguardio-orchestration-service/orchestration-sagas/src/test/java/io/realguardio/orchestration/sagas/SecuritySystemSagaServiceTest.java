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
    private CreateSecuritySystemWithLocationIdSaga createSecuritySystemWithLocationIdSaga;
    private PendingSecuritySystemResponses pendingResponses;
    private SecuritySystemSagaService service;

    @BeforeEach
    void setUp() {
        sagaInstanceFactory = mock(SagaInstanceFactory.class);
        createSecuritySystemWithLocationIdSaga = mock(CreateSecuritySystemWithLocationIdSaga.class);
        pendingResponses = new PendingSecuritySystemResponses();
        service = new SecuritySystemSagaService(sagaInstanceFactory,
                createSecuritySystemWithLocationIdSaga, pendingResponses);
    }

    @Test
    void shouldStartLocationIdSagaWhenCreatingWithLocationId() {
        Long locationId = 100L;
        String sagaId = "saga-456";

        SagaInstance sagaInstance = mock(SagaInstance.class);
        when(sagaInstance.getId()).thenReturn(sagaId);
        when(sagaInstanceFactory.create(eq(createSecuritySystemWithLocationIdSaga), any(CreateSecuritySystemWithLocationIdSagaData.class)))
                .thenReturn(sagaInstance);

        CompletableFuture<Long> future = service.createSecuritySystemWithLocationId(locationId);

        ArgumentCaptor<CreateSecuritySystemWithLocationIdSagaData> dataCaptor =
                ArgumentCaptor.forClass(CreateSecuritySystemWithLocationIdSagaData.class);
        verify(sagaInstanceFactory).create(eq(createSecuritySystemWithLocationIdSaga), dataCaptor.capture());

        CreateSecuritySystemWithLocationIdSagaData capturedData = dataCaptor.getValue();
        assertThat(capturedData.getLocationId()).isEqualTo(locationId);

        assertThat(service.hasPendingResponse(sagaId)).isTrue();
        assertThat(future).isNotNull();
    }

    @Test
    void shouldCompleteFutureWhenSecuritySystemCreated() {
        String sagaId = "saga-123";
        Long securitySystemId = 200L;
        Long locationId = 100L;

        SagaInstance sagaInstance = mock(SagaInstance.class);
        when(sagaInstance.getId()).thenReturn(sagaId);
        when(sagaInstanceFactory.create(eq(createSecuritySystemWithLocationIdSaga), any(CreateSecuritySystemWithLocationIdSagaData.class)))
                .thenReturn(sagaInstance);

        CompletableFuture<Long> future = service.createSecuritySystemWithLocationId(locationId);

        service.completeSecuritySystemCreation(sagaId, securitySystemId);

        assertThat(future.isDone()).isTrue();
        assertThat(future.getNow(null)).isEqualTo(securitySystemId);
        assertThat(service.hasPendingResponse(sagaId)).isFalse();
    }
}