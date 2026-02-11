package io.eventuate.examples.realguardio.securitysystemservice.domain;

public class LocationAlreadyHasSecuritySystemException extends RuntimeException {
    private final Long locationId;

    public LocationAlreadyHasSecuritySystemException(Long locationId) {
        super("Location " + locationId + " already has a SecuritySystem");
        this.locationId = locationId;
    }

    public Long getLocationId() {
        return locationId;
    }
}
