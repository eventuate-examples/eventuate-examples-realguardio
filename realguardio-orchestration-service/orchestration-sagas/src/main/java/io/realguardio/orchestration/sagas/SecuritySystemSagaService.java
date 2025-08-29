package io.realguardio.orchestration.sagas;

import io.eventuate.tram.sagas.orchestration.SagaInstanceFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecuritySystemSagaService {
    
    private final SagaInstanceFactory sagaInstanceFactory;
    private final CreateSecuritySystemSaga createSecuritySystemSaga;
    private final Map<String, CompletableFuture<Long>> pendingResponses = new ConcurrentHashMap<>();
    
    public SecuritySystemSagaService(SagaInstanceFactory sagaInstanceFactory,
                                    CreateSecuritySystemSaga createSecuritySystemSaga) {
        this.sagaInstanceFactory = sagaInstanceFactory;
        this.createSecuritySystemSaga = createSecuritySystemSaga;
    }
    
    public CompletableFuture<Long> createSecuritySystem(Long customerId, String locationName) {
        CreateSecuritySystemSagaData sagaData = new CreateSecuritySystemSagaData(customerId, locationName);
        CompletableFuture<Long> future = new CompletableFuture<>();
        
        var sagaInstance = sagaInstanceFactory.create(createSecuritySystemSaga, sagaData);
        pendingResponses.put(sagaInstance.getId(), future);
        
        return future;
    }
    
    public boolean hasPendingResponse(String sagaId) {
        return pendingResponses.containsKey(sagaId);
    }
    
    public void completeSecuritySystemCreation(String sagaId, Long securitySystemId) {
        CompletableFuture<Long> future = pendingResponses.remove(sagaId);
        if (future != null) {
            future.complete(securitySystemId);
        }
    }
}