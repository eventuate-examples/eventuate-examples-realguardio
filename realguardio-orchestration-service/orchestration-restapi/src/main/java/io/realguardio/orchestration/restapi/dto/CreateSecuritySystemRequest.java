package io.realguardio.orchestration.restapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSecuritySystemRequest(
    @NotNull(message = "Customer ID must not be null")
    Long customerId,
    
    @NotBlank(message = "Location name must not be blank")
    String locationName
) {
}