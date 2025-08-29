package io.realguardio.orchestration.sagas;

public class CreateSecuritySystemSagaData {
    
    private Long customerId;
    private String locationName;
    private Long securitySystemId;
    private Long locationId;
    private String rejectionReason;
    
    public CreateSecuritySystemSagaData() {
    }
    
    public CreateSecuritySystemSagaData(Long customerId, String locationName) {
        this.customerId = customerId;
        this.locationName = locationName;
    }
    
    public Long getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    
    public String getLocationName() {
        return locationName;
    }
    
    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
    
    public Long getSecuritySystemId() {
        return securitySystemId;
    }
    
    public void setSecuritySystemId(Long securitySystemId) {
        this.securitySystemId = securitySystemId;
    }
    
    public Long getLocationId() {
        return locationId;
    }
    
    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}