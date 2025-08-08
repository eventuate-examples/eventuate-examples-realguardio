package io.eventuate.examples.realguardio.securitysystemservice.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystems;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecuritySystemController {

  private final SecuritySystemService securitySystemService;

  public SecuritySystemController(SecuritySystemService securitySystemService) {
    this.securitySystemService = securitySystemService;
  }

  @GetMapping("/securitysystems")
  public SecuritySystems getSecuritySystems() {
    return new SecuritySystems(securitySystemService.findAll());
  }
}
