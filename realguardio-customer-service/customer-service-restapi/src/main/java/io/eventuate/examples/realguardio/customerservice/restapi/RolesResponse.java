package io.eventuate.examples.realguardio.customerservice.restapi;

import java.util.Set;

public class RolesResponse {
    
    private Set<String> roles;
    
    public RolesResponse() {
    }
    
    public RolesResponse(Set<String> roles) {
        this.roles = roles;
    }
    
    public Set<String> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}