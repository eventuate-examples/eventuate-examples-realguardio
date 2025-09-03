package io.eventuate.examples.realguardio.securitysystemservice.restapi;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAction;

public class SecuritySystemActionRequest {
    
    private SecuritySystemAction action;

    public SecuritySystemActionRequest() {
    }

    public SecuritySystemActionRequest(SecuritySystemAction action) {
        this.action = action;
    }

    public SecuritySystemAction getAction() {
        return action;
    }

    public void setAction(SecuritySystemAction action) {
        this.action = action;
    }
}