package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Set;

public class SecuritySystemProjectionImpl implements SecuritySystemProjection {
  private final long id;
  private final String locationName;
  private final SecuritySystemState securitySystemState;
  private final Set<SecuritySystemWithActions> actions;

  public SecuritySystemProjectionImpl(long id, String locationName, SecuritySystemState securitySystemState, Set<SecuritySystemWithActions> actions) {
    this.id = id;
    this.locationName = locationName;
    this.securitySystemState = securitySystemState;
    this.actions = actions;
  }

  public Set<SecuritySystemWithActions> getActions() {
    return actions;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getLocationName() {
    return locationName;
  }

  @Override
  public String getState() {
    return securitySystemState.name();
  }

  @Override
  public Long getLocationId() {
    return 0L;
  }

  @Override
  public String getRejectionReason() {
    return "";
  }

  @Override
  public Long getVersion() {
    return 0L;
  }

  @Override
  public String[] getRoleNames() {
    return new String[0];
  }

}
