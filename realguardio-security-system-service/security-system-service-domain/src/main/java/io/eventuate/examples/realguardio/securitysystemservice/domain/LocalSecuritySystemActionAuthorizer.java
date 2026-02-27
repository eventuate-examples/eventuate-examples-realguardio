package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@Profile("!UseOsoService")
public class LocalSecuritySystemActionAuthorizer extends AbstractSecuritySystemActionAuthorizer {

  private static final Logger logger = LoggerFactory.getLogger(LocalSecuritySystemActionAuthorizer.class);

  private final SecuritySystemRepository securitySystemRepository;
  private final CustomerServiceClient customerServiceClient;

  public LocalSecuritySystemActionAuthorizer(CustomerServiceClient customerServiceClient, SecuritySystemRepository securitySystemRepository, UserNameSupplier userNameSupplier) {
    super(userNameSupplier);
    this.customerServiceClient = customerServiceClient;
    this.securitySystemRepository = securitySystemRepository;
  }


  @Override
  protected void isAllowedForCustomerEmployee(String permission, long securitySystemId) {
      Set<String> requiredRoles = RolesAndPermissions.rolesForPermission(permission);
      SecuritySystem securitySystem = securitySystemRepository.findById(securitySystemId)
          .orElseThrow(() -> new NotFoundException("Security system not found: " + (Long) securitySystemId));

      Long locationId = securitySystem.getLocationId();

      String userId = userNameSupplier.getCurrentUserName();

      Set<String> rolesAtLocation = customerServiceClient.getUserRolesAtLocation(userId, locationId);

      if (Collections.disjoint(rolesAtLocation, requiredRoles)) {
        logger.warn("User {} lacks {} permission for location {}. Only has {}", userId, requiredRoles, locationId, rolesAtLocation);
        throw new ForbiddenException(
            String.format("User %s lacks %s permission for location %d. Only has %s",
                    userId, requiredRoles, locationId, rolesAtLocation)
        );
      }
  }


}
