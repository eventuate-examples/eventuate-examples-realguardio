package io.realguardio.osointegration.ososervice;

import org.springframework.stereotype.Service;

@Service
public class RealGuardOsoAuthorizer {

  private final OsoService osoService;

  public RealGuardOsoAuthorizer(OsoService osoService) {
    this.osoService = osoService;
  }

  public boolean isAuthorized(String user, String action, String securitySystem) {
    return osoService.authorize("CustomerEmployee", user, action, "SecuritySystem", securitySystem);
  }

}
