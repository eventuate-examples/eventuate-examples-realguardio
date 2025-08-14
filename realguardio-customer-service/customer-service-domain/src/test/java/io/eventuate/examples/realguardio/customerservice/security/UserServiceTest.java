package io.eventuate.examples.realguardio.customerservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes=UserServiceTest.Config.class)
public class UserServiceTest {

    @Configuration
    @Import({SecurityConfiguration.class})
    @EnableAutoConfiguration
    public static class Config {
    }

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserDetailsManager userDetailsManager;

    @Test
    void shouldCreateCustomerEmployeeUser() {
        userService.createCustomerEmployeeUser("test@realguard.io");
        
        assertThat(userService.userExists("test@realguard.io")).isTrue();
        
        // Verify user has correct role
        UserDetails user = userDetailsManager.loadUserByUsername("test@realguard.io");
        assertThat(user.getAuthorities())
            .anyMatch(a -> a.getAuthority().equals("ROLE_REALGUARDIO_CUSTOMER_EMPLOYEE"));
    }
    
    @Test
    void shouldCheckIfUserExists() {
        assertThat(userService.userExists("nonexistent@realguard.io")).isFalse();
        
        userService.createCustomerEmployeeUser("exists@realguard.io");
        assertThat(userService.userExists("exists@realguard.io")).isTrue();
    }
}