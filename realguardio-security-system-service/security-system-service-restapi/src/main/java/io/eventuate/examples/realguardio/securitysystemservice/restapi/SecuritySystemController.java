package io.eventuate.examples.realguardio.securitysystemservice.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystems;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
  
  @PutMapping("/securitysystems/{id}")
  @PreAuthorize("hasRole('REALGUARDIO_ADMIN') or hasRole('REALGUARDIO_CUSTOMER_EMPLOYEE')")
  public ResponseEntity<SecuritySystem> updateSecuritySystem(
          @PathVariable("id") Long id,
          @RequestBody SecuritySystemActionRequest request) {
      
      SecuritySystem updated;
      
      if (request.getAction() == SecuritySystemAction.ARM) {
          updated = securitySystemService.arm(id);
      } else if (request.getAction() == SecuritySystemAction.DISARM) {
          updated = securitySystemService.disarm(id);
      } else {
          return ResponseEntity.badRequest().build();
      }
      
      return ResponseEntity.ok(updated);
  }
}
