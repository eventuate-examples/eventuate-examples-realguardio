package io.eventuate.examples.realguardio.customerservice.osointegration;

import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.CustomerActionAuthorizer;
import io.eventuate.examples.realguardio.customerservice.customermanagement.domain.ForbiddenException;
import io.eventuate.examples.realguardio.customerservice.security.UserNameSupplier;
import io.realguardio.osointegration.ososervice.RealGuardOsoAuthorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class OsoCustomerActionAuthorizer implements CustomerActionAuthorizer {

  private static final Logger logger = LoggerFactory.getLogger(OsoCustomerActionAuthorizer.class);

  private final UserNameSupplier userNameSupplier;
  private final RealGuardOsoAuthorizer realGuardOsoAuthorizer;

  public OsoCustomerActionAuthorizer(UserNameSupplier userNameSupplier, RealGuardOsoAuthorizer realGuardOsoAuthorizer) {
    this.userNameSupplier = userNameSupplier;
    this.realGuardOsoAuthorizer = realGuardOsoAuthorizer;
  }


  @Override
  public void isAllowed(String permission, long customerId) {

    String userId = userNameSupplier.getCurrentUserEmail();
    if (!isAuthorized(userId, permission, String.valueOf(customerId))) {
      logger.warn("User {} lacks {} permission for customerId {}", userId, permission, customerId);
      throw new ForbiddenException(
          String.format("User %s is not authorized to %s customer %d",
              userId, permission, customerId));
    }

  }

    private boolean isAuthorized(String user, String action, String securitySystem) {
        try {
            return realGuardOsoAuthorizer.isAuthorized(user, action, "Customer", securitySystem).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
