package io.eventuate.examples.realguardio.securitysystemservice.persistence.finder.oso;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemProjection;

public record SecuritySystemProjectionImpl(
        Long id,
        String locationName,
        String state,
        Long locationId,
        String rejectionReason,
        Long version,
        String[] roleNames
) implements SecuritySystemProjection {

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getLocationName() {
        return locationName;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public Long getLocationId() {
        return locationId;
    }

    @Override
    public String getRejectionReason() {
        return rejectionReason;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    @Override
    public String[] getRoleNames() {
        return roleNames;
    }
}
