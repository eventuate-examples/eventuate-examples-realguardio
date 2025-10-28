package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
  public void verifyCanDo(long securitySystemId, String permission) {
        validateLocationPermission(securitySystemId, RolesAndPermissions.rolesForPermission(permission));
  }


 private void validateLocationPermission(Long securitySystemID, Set<String> requiredRoles) {
    SecuritySystem securitySystem = securitySystemRepository.findById(securitySystemID)
        .orElseThrow(() -> new NotFoundException("Security system not found: " + securitySystemID));

    Long locationId = securitySystem.getLocationId();

    String userId = userNameSupplier.getCurrentUserName();

    Set<String> roles = customerServiceClient.getUserRolesAtLocation(userId, locationId);

    if (Collections.disjoint(roles, requiredRoles)) {
      logger.warn("User {} lacks {} permission for location {}", userId, requiredRoles, locationId);
      throw new ForbiddenException(
          String.format("User lacks %s permission for location %d",
              requiredRoles, locationId)
      );
    }
  }

}
