package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.Set;

public record SecuritySystem(long id, String locationName, SecuritySystemState state, Set<SecuritySystemAction> actions) {

}
