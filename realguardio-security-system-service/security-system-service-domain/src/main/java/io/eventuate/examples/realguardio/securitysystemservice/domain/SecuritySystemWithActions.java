package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Set;

public record SecuritySystemWithActions(Long id, String locationName, SecuritySystemState state, Long locationId,
                                        String rejectionReason, Long version, Set<SecuritySystemAction> actions) {

  public SecuritySystemWithActions(String locationName, SecuritySystemState state, Set<SecuritySystemAction> actions) {
    this(null, locationName, state, null, null, null, actions);
  }

  public SecuritySystemWithActions(long id, String locationName, SecuritySystemState state, Set<SecuritySystemAction> actions) {
    this(id, locationName, state, null, null, null, actions);
  }
}
