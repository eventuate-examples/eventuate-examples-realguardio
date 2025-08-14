package io.eventuate.examples.realguardio.customerservice.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

public class UserService {
    
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }
    
    public void createCustomerEmployeeUser(String email) {
        UserDetails user = User.builder()
            .username(email)
            .password(passwordEncoder.encode("changeme"))  // Default password
            .roles("REALGUARDIO_CUSTOMER_EMPLOYEE")
            .build();
        
        userDetailsManager.createUser(user);
    }
    
    public boolean userExists(String email) {
        return userDetailsManager.userExists(email);
    }
}