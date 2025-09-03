package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.List;

public interface SecuritySystemService {
    List<SecuritySystem> findAll();
    
    Long createSecuritySystem(String locationName);
    
    void noteLocationCreated(Long securitySystemId, Long locationId);
    
    void updateCreationFailed(Long securitySystemId, String rejectionReason);
    
    SecuritySystem arm(Long id);
    
    SecuritySystem disarm(Long id);
}