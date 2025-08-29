package io.realguardio.orchestration.sagas;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PendingSecuritySystemResponses {
    private final Map<String, CompletableFuture<Long>> pendingResponses = new ConcurrentHashMap<>();

    public CompletableFuture<Long> createPendingResponse(String sagaId) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        pendingResponses.put(sagaId, future);
        return future;
    }

    public void completeSecuritySystemCreation(String sagaId, Long securitySystemId) {
        CompletableFuture<Long> future = pendingResponses.remove(sagaId);
        if (future != null) {
            future.complete(securitySystemId);
        }
    }
    
    public boolean containsKey(String sagaId) {
        return pendingResponses.containsKey(sagaId);
    }
}