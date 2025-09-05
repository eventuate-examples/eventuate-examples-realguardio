package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class SecuritySystemServiceImpl implements SecuritySystemService {
    
    private final SecuritySystemRepository securitySystemRepository;
    private final CustomerServiceClient customerServiceClient;
    private final UserNameSupplier userNameSupplier;
    
    public SecuritySystemServiceImpl(SecuritySystemRepository securitySystemRepository,
                                    CustomerServiceClient customerServiceClient,
                                    UserNameSupplier userNameSupplier) {
        if (securitySystemRepository == null) {
            throw new IllegalArgumentException("securitySystemRepository cannot be null");
        }
        if (customerServiceClient == null) {
            throw new IllegalArgumentException("customerServiceClient cannot be null");
        }
        if (userNameSupplier == null) {
            throw new IllegalArgumentException("userNameSupplier cannot be null");
        }
        this.securitySystemRepository = securitySystemRepository;
        this.customerServiceClient = customerServiceClient;
        this.userNameSupplier = userNameSupplier;
    }
    
    @Override
    public List<SecuritySystem> findAll() {
        return securitySystemRepository.findAll();
    }
    
    @Override
    public Optional<SecuritySystem> findById(Long id) {
        return securitySystemRepository.findById(id);
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
        SecuritySystem securitySystem = securitySystemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Security system not found: " + id));
        
        if (securitySystem.getLocationId() == null) {
            throw new BadRequestException("Security system not properly configured: missing location");
        }
        
        // Check location-based authorization for customer employees
        if (userNameSupplier.isCustomerEmployee()) {
            validateLocationPermission(securitySystem.getLocationId(), "CAN_ARM");
        }
        
        securitySystem.arm();
        return securitySystemRepository.save(securitySystem);
    }
    
    @Override
    public SecuritySystem disarm(Long id) {
        SecuritySystem securitySystem = securitySystemRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Security system not found: " + id));
        
        if (securitySystem.getLocationId() == null) {
            throw new BadRequestException("Security system not properly configured: missing location");
        }
        
        // Check location-based authorization for customer employees
        if (userNameSupplier.isCustomerEmployee()) {
            validateLocationPermission(securitySystem.getLocationId(), "CAN_DISARM");
        }
        
        securitySystem.disarm();
        return securitySystemRepository.save(securitySystem);
    }
    
    private void validateLocationPermission(Long locationId, String requiredRole) {
        String userId = userNameSupplier.getCurrentUserName();
        
        try {
            Set<String> roles = customerServiceClient.getUserRolesAtLocation(userId, locationId);
            
            if (!roles.contains(requiredRole)) {
                throw new ForbiddenException(
                    String.format("User lacks %s permission for location %d", 
                                requiredRole, locationId)
                );
            }
        } catch (ForbiddenException e) {
            throw e; // Re-throw ForbiddenException as-is
        } catch (Exception e) {
            throw new ServiceUnavailableException("Authorization service temporarily unavailable", e);
        }
    }
}