package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final SecuritySystemActionAuthorizer securitySystemActionAuthorizer;
    private final SecuritySystemFinder securitySystemFinder;
    private final SecuritySystemEventPublisher securitySystemEventPublisher;

    public SecuritySystemServiceImpl(SecuritySystemRepository securitySystemRepository,
                                    CustomerServiceClient customerServiceClient,
                                    UserNameSupplier userNameSupplier,
                                    SecuritySystemActionAuthorizer securitySystemActionAuthorizer,
                                    SecuritySystemFinder securitySystemFinder,
                                    SecuritySystemEventPublisher securitySystemEventPublisher) {
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
        this.securitySystemActionAuthorizer = securitySystemActionAuthorizer;
        this.securitySystemFinder = securitySystemFinder;
        this.securitySystemEventPublisher = securitySystemEventPublisher;
    }
    
    @Override
    public List<SecuritySystemWithActions> findAll() {
        if (userNameSupplier.isCustomerEmployee())
            return securitySystemFinder.findAllAccessible(userNameSupplier.getCurrentUserName())
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
        Optional<SecuritySystem> securitySystem = securitySystemRepository.findById(id);

        // Check location-based authorization for customer employees
        if (userNameSupplier.isCustomerEmployee() && securitySystem.map(ss -> ss.getLocationId() != null).orElse(false)) {
            securitySystemActionAuthorizer.verifyCanDo(id, RolesAndPermissions.VIEW);
        }
        return securitySystem;
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
    public Long createSecuritySystemWithLocation(Long locationId, String locationName) {
        SecuritySystem securitySystem = new SecuritySystem(locationName, SecuritySystemState.DISARMED);
        securitySystem.setLocationId(locationId);
        try {
            SecuritySystem savedSystem = securitySystemRepository.save(securitySystem);
            securitySystemEventPublisher.publish(savedSystem, new SecuritySystemAssignedToLocation(savedSystem.getId(), locationId));
            return savedSystem.getId();
        } catch (DataIntegrityViolationException e) {
            throw new LocationAlreadyHasSecuritySystemException(locationId);
        }
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
            securitySystemActionAuthorizer.verifyCanDo(id, RolesAndPermissions.ARM);
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
            securitySystemActionAuthorizer.verifyCanDo(id, RolesAndPermissions.DISARM);
        }
        
        securitySystem.disarm();
        return securitySystemRepository.save(securitySystem);
    }

}