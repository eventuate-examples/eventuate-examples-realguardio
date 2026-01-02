package io.realguardio.orchestration.sagas;

public class CreateSecuritySystemWithLocationIdSagaData {

    private String sagaId;
    private Long locationId;
    private String locationName;
    private Long customerId;
    private Long securitySystemId;
    private String rejectionReason;

    public CreateSecuritySystemWithLocationIdSagaData() {
    }

    public CreateSecuritySystemWithLocationIdSagaData(Long locationId) {
        this.locationId = locationId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSecuritySystemId() {
        return securitySystemId;
    }

    public void setSecuritySystemId(Long securitySystemId) {
        this.securitySystemId = securitySystemId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
