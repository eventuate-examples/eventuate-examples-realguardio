package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.List;
import java.util.Optional;

public interface SecuritySystemService {
    List<SecuritySystem> findAll();
    
    Optional<SecuritySystem> findById(Long id);
    
    Long createSecuritySystem(String locationName);
    
    void noteLocationCreated(Long securitySystemId, Long locationId);
    
    void updateCreationFailed(Long securitySystemId, String rejectionReason);
    
    SecuritySystem arm(Long id);
    
    SecuritySystem disarm(Long id);
}