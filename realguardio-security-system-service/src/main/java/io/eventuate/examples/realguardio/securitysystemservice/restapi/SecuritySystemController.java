package io.eventuate.examples.realguardio.securitysystemservice.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystems;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
public class SecuritySystemController {

  @GetMapping("/securitysystems")
  public SecuritySystems getSecuritySystems() {
    return new SecuritySystems(List.of(
        new SecuritySystem(1, "Oakland office", SecuritySystemState.ARMED, Set.of(SecuritySystemAction.DISARM)),
        new SecuritySystem(2, "Berkeley office", SecuritySystemState.DISARMED, Set.of(SecuritySystemAction.ARM)),
        new SecuritySystem(3, "Hayward office", SecuritySystemState.ALARMED, Set.of(SecuritySystemAction.DISARM, SecuritySystemAction.ACKNOWLEDGE))
    ));
  }
}
