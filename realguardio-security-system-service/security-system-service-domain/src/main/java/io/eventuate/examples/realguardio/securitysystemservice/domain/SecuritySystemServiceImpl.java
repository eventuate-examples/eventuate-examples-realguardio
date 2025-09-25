package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class SecuritySystemServiceImpl implements SecuritySystemService {

    private static final Logger logger = LoggerFactory.getLogger(SecuritySystemServiceImpl.class);

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
    public List<SecuritySystemWithActions> findAll() {
        if (userNameSupplier.isCustomerEmployee())
            return securitySystemRepository.findAllAccessible(userNameSupplier.getCurrentUserName())
                .stream()
                .map(SecuritySystemProjection::toSecuritySystemWithActions)
                .toList();
        else
            return securitySystemRepository.findAll().stream()
                .map(this::toSecuritySystemWithActions)
                .toList();
    }

    private SecuritySystemWithActions toSecuritySystemWithActions(SecuritySystem securitySystem) {
        return new SecuritySystemWithActions(

            securitySystem.getLocationName(), securitySystem.getState(),
            Set.of(SecuritySystemAction.ARM, SecuritySystemAction.DISARM)
        );
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
            validateLocationPermission(securitySystem.getLocationId(), "SECURITY_SYSTEM_ARMER");
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
            validateLocationPermission(securitySystem.getLocationId(), "SECURITY_SYSTEM_DISARMER");
        }
        
        securitySystem.disarm();
        return securitySystemRepository.save(securitySystem);
    }
    
    private void validateLocationPermission(Long locationId, String requiredRole) {
        String userId = userNameSupplier.getCurrentUserName();

        Set<String> roles = customerServiceClient.getUserRolesAtLocation(userId, locationId);

        if (!roles.contains(requiredRole)) {
            logger.warn("User {} lacks {} permission for location {}", userId, requiredRole, locationId);
            throw new ForbiddenException(
                String.format("User lacks %s permission for location %d",
                    requiredRole, locationId)
            );
        }
    }
}