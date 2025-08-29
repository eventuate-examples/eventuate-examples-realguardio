package io.realguardio.orchestration.restapi;

import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemRequest;
import io.realguardio.orchestration.restapi.dto.CreateSecuritySystemResponse;
import io.realguardio.orchestration.sagas.SecuritySystemSagaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/securitysystems")
public class SecuritySystemController {
    
    private final SecuritySystemSagaService securitySystemSagaService;
    
    public SecuritySystemController(SecuritySystemSagaService securitySystemSagaService) {
        this.securitySystemSagaService = securitySystemSagaService;
    }
    
    @PostMapping
    public CompletableFuture<ResponseEntity<CreateSecuritySystemResponse>> createSecuritySystem(
            @Valid @RequestBody CreateSecuritySystemRequest request) {
        
        return securitySystemSagaService
                .createSecuritySystem(request.customerId(), request.locationName())
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(securitySystemId -> 
                    ResponseEntity.status(HttpStatus.CREATED)
                        .body(new CreateSecuritySystemResponse(securitySystemId)))
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof TimeoutException) {
                        throw new ServiceUnavailableException("Service temporarily unavailable");
                    }
                    throw new RuntimeException(ex);
                });
    }
}