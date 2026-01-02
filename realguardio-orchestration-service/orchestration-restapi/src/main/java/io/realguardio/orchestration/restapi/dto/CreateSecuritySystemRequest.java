package io.realguardio.orchestration.restapi.dto;

public record CreateSecuritySystemRequest(
    Long customerId,
    String locationName,
    Long locationId
) {
    public boolean usesLocationIdFlow() {
        return locationId != null;
    }

    public boolean usesLegacyFlow() {
        return customerId != null && locationName != null && !locationName.isBlank();
    }

    public boolean isValid() {
        return usesLocationIdFlow() || usesLegacyFlow();
    }
}