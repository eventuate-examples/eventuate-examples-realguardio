package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("!UseOsoService")
public class LocalSecuritySystemActionAuthorizer implements SecuritySystemActionAuthorizer {

  private static final Logger logger = LoggerFactory.getLogger(LocalSecuritySystemActionAuthorizer.class);

  private final SecuritySystemRepository securitySystemRepository;
  private final CustomerServiceClient customerServiceClient;
  private final UserNameSupplier userNameSupplier;

  public LocalSecuritySystemActionAuthorizer(CustomerServiceClient customerServiceClient, SecuritySystemRepository securitySystemRepository, UserNameSupplier userNameSupplier) {
    this.customerServiceClient = customerServiceClient;
    this.securitySystemRepository = securitySystemRepository;
    this.userNameSupplier = userNameSupplier;
  }

  @Override
  public void verifyCanArm(long securitySystemId) {
    validateLocationPermission(securitySystemId, "SECURITY_SYSTEM_ARMER");
  }

  @Override
  public void verifyCanDisarm(long securitySystemId) {
    validateLocationPermission(securitySystemId, "SECURITY_SYSTEM_DISARMER");
  }

  private void validateLocationPermission(Long securitySystemID, String requiredRole) {
    SecuritySystem securitySystem = securitySystemRepository.findById(securitySystemID)
        .orElseThrow(() -> new NotFoundException("Security system not found: " + securitySystemID));

    Long locationId = securitySystem.getLocationId();

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
