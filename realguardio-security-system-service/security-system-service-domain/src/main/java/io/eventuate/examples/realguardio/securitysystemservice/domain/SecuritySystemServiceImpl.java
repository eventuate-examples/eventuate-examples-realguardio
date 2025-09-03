package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SecuritySystemServiceImpl implements SecuritySystemService {
    
    private final SecuritySystemRepository securitySystemRepository;
    
    public SecuritySystemServiceImpl(SecuritySystemRepository securitySystemRepository) {
        if (securitySystemRepository == null) {
            throw new IllegalArgumentException("securitySystemRepository cannot be null");
        }
        this.securitySystemRepository = securitySystemRepository;
    }
    
    @Override
    public List<SecuritySystem> findAll() {
        return securitySystemRepository.findAll();
    }
    
    @Override
    public Long createSecuritySystem(String locationName) {
        SecuritySystem securitySystem = new SecuritySystem(locationName, SecuritySystemState.CREATION_PENDING);
        SecuritySystem savedSystem = securitySystemRepository.save(securitySystem);
        return savedSystem.getId();
    }
    
    @Override
    public void noteLocationCreated(Long securitySystemId, Long locationId) {
        SecuritySystem securitySystem = securitySystemRepository.findById(securitySystemId)
            .orElseThrow(() -> new IllegalArgumentException("Security system not found: " + securitySystemId));
        securitySystem.setLocationId(locationId);
        securitySystem.setState(SecuritySystemState.DISARMED);
        securitySystemRepository.save(securitySystem);
    }
    
    @Override
    public void updateCreationFailed(Long securitySystemId, String rejectionReason) {
        SecuritySystem securitySystem = securitySystemRepository.findById(securitySystemId)
            .orElseThrow(() -> new IllegalArgumentException("Security system not found: " + securitySystemId));
        securitySystem.setState(SecuritySystemState.CREATION_FAILED);
        securitySystem.setRejectionReason(rejectionReason);
        securitySystemRepository.save(securitySystem);
    }
    
    @Override
    public SecuritySystem arm(Long id) {
        throw new UnsupportedOperationException("Implement me");
    }
    
    @Override
    public SecuritySystem disarm(Long id) {
        throw new UnsupportedOperationException("Implement me");
    }
}