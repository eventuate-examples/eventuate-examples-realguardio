package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public interface SecuritySystemProjection {
  Long getId();
  String getLocationName();
  String getState();
  Long getLocationId();
  String getRejectionReason();
  Long getVersion();

  // Postgres ARRAY_AGG maps nicely to String[].
  String[] getRoleNames();


  default SecuritySystemWithActions toSecuritySystemWithActions() {
    Set<String> roles = new HashSet<>(Arrays.asList(getRoleNames()));
    SecuritySystemState state = SecuritySystemState.valueOf(getState());
    return new SecuritySystemWithActions(getId(), getLocationName(), state, getLocationId(), getRejectionReason(), getVersion(), allowedActions(state, roles));
  }

  default Set<SecuritySystemAction> allowedActions(SecuritySystemState state, Set<String> roles) {
    Set<SecuritySystemAction> applicableActions = new HashSet<>(state.allowedActions());
    Set<SecuritySystemAction> actionsForRole = roles.stream().flatMap(r -> actionForRole(r).stream()).collect(toSet());
    applicableActions.retainAll(actionsForRole);
    return applicableActions;
  }

  default Set<SecuritySystemAction> actionForRole(String role) {
    return switch (role) {
      case "SECURITY_SYSTEM_DISARMER" -> Set.of(SecuritySystemAction.DISARM);
      case "SECURITY_SYSTEM_ARMER" -> Set.of(SecuritySystemAction.ARM);
      case "SECURITY_SYSTEM_ACKNOWLEDGER" -> Set.of(SecuritySystemAction.ACKNOWLEDGE);
      default -> Set.of();
    };
  }
}
