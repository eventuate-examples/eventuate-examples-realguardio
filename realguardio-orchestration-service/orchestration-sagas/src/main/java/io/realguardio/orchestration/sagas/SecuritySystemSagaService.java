package io.realguardio.orchestration.sagas;

import io.eventuate.tram.sagas.orchestration.SagaInstanceFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SecuritySystemSagaService {

    private final SagaInstanceFactory sagaInstanceFactory;
    private final CreateSecuritySystemSaga createSecuritySystemSaga;
    private final CreateSecuritySystemWithLocationIdSaga createSecuritySystemWithLocationIdSaga;
    private final PendingSecuritySystemResponses pendingResponses;

    public SecuritySystemSagaService(SagaInstanceFactory sagaInstanceFactory,
                                    CreateSecuritySystemSaga createSecuritySystemSaga,
                                    CreateSecuritySystemWithLocationIdSaga createSecuritySystemWithLocationIdSaga,
                                    PendingSecuritySystemResponses pendingResponses) {
        this.sagaInstanceFactory = sagaInstanceFactory;
        this.createSecuritySystemSaga = createSecuritySystemSaga;
        this.createSecuritySystemWithLocationIdSaga = createSecuritySystemWithLocationIdSaga;
        this.pendingResponses = pendingResponses;
    }

    public CompletableFuture<Long> createSecuritySystem(Long customerId, String locationName) {
        CreateSecuritySystemSagaData sagaData = new CreateSecuritySystemSagaData(customerId, locationName);

        var sagaInstance = sagaInstanceFactory.create(createSecuritySystemSaga, sagaData);
        CompletableFuture<Long> future = pendingResponses.createPendingResponse(sagaInstance.getId());

        return future;
    }

    public CompletableFuture<Long> createSecuritySystemWithLocationId(Long locationId) {
        CreateSecuritySystemWithLocationIdSagaData sagaData = new CreateSecuritySystemWithLocationIdSagaData(locationId);

        var sagaInstance = sagaInstanceFactory.create(createSecuritySystemWithLocationIdSaga, sagaData);
        CompletableFuture<Long> future = pendingResponses.createPendingResponse(sagaInstance.getId());

        return future;
    }

    public boolean hasPendingResponse(String sagaId) {
        return pendingResponses.containsKey(sagaId);
    }

    public void completeSecuritySystemCreation(String sagaId, Long securitySystemId) {
        pendingResponses.completeSecuritySystemCreation(sagaId, securitySystemId);
    }
}