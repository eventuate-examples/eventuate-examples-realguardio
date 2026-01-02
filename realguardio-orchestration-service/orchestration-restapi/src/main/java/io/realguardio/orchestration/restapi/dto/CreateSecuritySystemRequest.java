package io.realguardio.orchestration.restapi.dto;

public record CreateSecuritySystemRequest(
    Long locationId
) {
    public boolean isValid() {
        return locationId != null;
    }
}