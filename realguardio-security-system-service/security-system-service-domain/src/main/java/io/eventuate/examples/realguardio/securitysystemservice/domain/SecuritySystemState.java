package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Set;

public enum SecuritySystemState {
  CREATION_PENDING {
    @Override
    public Set<SecuritySystemAction> allowedActions() {
      return Set.of();
    }
  }, CREATION_FAILED {
    @Override
    public Set<SecuritySystemAction> allowedActions() {
      return Set.of();
    }
  }, DISARMED {
    @Override
    public Set<SecuritySystemAction> allowedActions() {
      return Set.of(SecuritySystemAction.ARM);
    }
  }, ARMED {
    @Override
    public Set<SecuritySystemAction> allowedActions() {
      return Set.of(SecuritySystemAction.DISARM);
    }
  }, ALARMED {
    @Override
    public Set<SecuritySystemAction> allowedActions() {
      return Set.of(SecuritySystemAction.DISARM, SecuritySystemAction.ACKNOWLEDGE);
    }
  };

  public abstract Set<SecuritySystemAction> allowedActions();
}
