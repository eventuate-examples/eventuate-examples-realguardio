package io.eventuate.examples.realguardio.securitysystemservice.domain;

import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsoSecuritySystemActionAuthorizer implements SecuritySystemActionAuthorizer {

  private static final Logger logger = LoggerFactory.getLogger(OsoSecuritySystemActionAuthorizer.class);

  private final UserNameSupplier userNameSupplier;
  private final RealGuardOsoAuthorizer realGuardOsoAuthorizer;

  public OsoSecuritySystemActionAuthorizer(UserNameSupplier userNameSupplier, RealGuardOsoAuthorizer realGuardOsoAuthorizer) {
    this.userNameSupplier = userNameSupplier;
    this.realGuardOsoAuthorizer = realGuardOsoAuthorizer;
  }

  @Override
  public void verifyCanArm(long securitySystemId) {
    verifyCanDo(securitySystemId, "arm");
  }

  @Override
  public void verifyCanDisarm(long securitySystemId) {
    verifyCanDo(securitySystemId, "disarm");
  }

  private void verifyCanDo(long securitySystemId, String permission) {

    String userId = userNameSupplier.getCurrentUserName();
    if (!realGuardOsoAuthorizer.isAuthorized(userId, permission, String.valueOf(securitySystemId))) {
      logger.warn("User {} lacks {} permission for securitySystemId {}", userId, permission, securitySystemId);
      throw new ForbiddenException(
          String.format("User %s is not authorized to %s security system %d",
              userId, permission, securitySystemId)
      );
    }

  }

}
