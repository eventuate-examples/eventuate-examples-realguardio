package io.realguardio.orchestration.sagas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PendingSecuritySystemResponses {

    private static final Logger logger = LoggerFactory.getLogger(PendingSecuritySystemResponses.class);

    private final Map<String, CompletableFuture<Long>> pendingResponses = new ConcurrentHashMap<>();

    public CompletableFuture<Long> createPendingResponse(String sagaId) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        pendingResponses.put(sagaId, future);
        return future;
    }

    public void completeSecuritySystemCreation(String sagaId, Long securitySystemId) {
        if (sagaId == null) {
            logger.warn("sagaId is null, cannot complete security system creation for securitySystemId {}", securitySystemId);
            return;
        }

        CompletableFuture<Long> future = pendingResponses.remove(sagaId);
        logger.info("Completing security system creation for sagaId {}: {} future: {}", sagaId, securitySystemId, future != null);
        if (future != null) {
            future.complete(securitySystemId);
        }
    }
    
    public boolean containsKey(String sagaId) {
        return pendingResponses.containsKey(sagaId);
    }
}