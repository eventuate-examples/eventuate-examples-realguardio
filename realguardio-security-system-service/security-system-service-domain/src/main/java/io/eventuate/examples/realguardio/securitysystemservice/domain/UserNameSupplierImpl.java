package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserNameSupplierImpl implements UserNameSupplier {
    
    @Override
    public String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getName();
    }
    
    @Override
    public Set<String> getCurrentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }
    
    @Override
    public boolean isCustomerEmployee() {
        return getCurrentUserRoles().contains("ROLE_REALGUARDIO_CUSTOMER_EMPLOYEE");
    }
}