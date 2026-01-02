package io.realguardio.orchestration.sagas;

public class LocationNotFoundException extends RuntimeException {

    private final Long locationId;

    public LocationNotFoundException(Long locationId) {
        super("Location not found: " + locationId);
        this.locationId = locationId;
    }

    public Long getLocationId() {
        return locationId;
    }
}
