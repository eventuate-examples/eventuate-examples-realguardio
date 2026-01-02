package io.realguardio.orchestration.restapi;

import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemRequest;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemResponse;
import io.realguardio.orchestration.sagas.SecuritySystemSagaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/securitysystems")
public class SecuritySystemController {

    private static final Logger logger = LoggerFactory.getLogger(SecuritySystemController.class);

    private final SecuritySystemSagaService securitySystemSagaService;

    public SecuritySystemController(SecuritySystemSagaService securitySystemSagaService) {
        this.securitySystemSagaService = securitySystemSagaService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<CreateSecuritySystemResponse>> createSecuritySystem(
            @RequestBody CreateSecuritySystemRequest request) {

        if (!request.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either locationId or (customerId + locationName) must be provided");
        }

        CompletableFuture<Long> sagaFuture;
        if (request.usesLocationIdFlow()) {
            logger.info("Creating security system with locationId: {}", request.locationId());
            sagaFuture = securitySystemSagaService.createSecuritySystemWithLocationId(request.locationId());
        } else {
            logger.info("Creating security system with customerId: {} and locationName: {}",
                    request.customerId(), request.locationName());
            sagaFuture = securitySystemSagaService.createSecuritySystem(request.customerId(), request.locationName());
        }

        return sagaFuture
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(securitySystemId -> {
                    logger.info("security system created {}", securitySystemId);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(new CreateSecuritySystemResponse(securitySystemId));
                })
                .exceptionally(ex -> {
                    logger.error("Error creating security system: " + ex.getMessage(), ex);
                    if (ex.getCause() instanceof TimeoutException) {
                        throw new ServiceUnavailableException("Service temporarily unavailable");
                    }
                    throw new RuntimeException(ex);
                });
    }
}