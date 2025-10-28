package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.ForbiddenException;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemActionAuthorizer;
import io.eventuate.examples.realguardio.securitysystemservice.domain.UserNameSupplier;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

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

  @Override
  public void verifyCanView(long securitySystemId) {
     verifyCanDo(securitySystemId, "view");
  }

    private void verifyCanDo(long securitySystemId, String permission) {

    String userId = userNameSupplier.getCurrentUserName();
    if (!isAuthorized(userId, permission, String.valueOf(securitySystemId))) {
      logger.warn("User {} lacks {} permission for securitySystemId {}", userId, permission, securitySystemId);
      throw new ForbiddenException(
          String.format("User %s is not authorized to %s security system %d",
              userId, permission, securitySystemId)
      );
    }

  }

    private boolean isAuthorized(String user, String action, String securitySystem) {
        try {
            return realGuardOsoAuthorizer.isAuthorized(user, action, securitySystem).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
