package io.realguardio.orchestration.sagas;

public class LocationAlreadyHasSecuritySystemException extends RuntimeException {

    private final Long locationId;

    public LocationAlreadyHasSecuritySystemException(Long locationId) {
        super("Location already has a SecuritySystem");
        this.locationId = locationId;
    }

    public Long getLocationId() {
        return locationId;
    }
}
