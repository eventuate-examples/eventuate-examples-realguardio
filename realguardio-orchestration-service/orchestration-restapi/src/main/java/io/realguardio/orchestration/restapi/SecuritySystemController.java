package io.realguardio.orchestration.restapi;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/securitysystems")
public class SecuritySystemController {
    
    @PostMapping
    public void createSecuritySystem() {
        
    }
}