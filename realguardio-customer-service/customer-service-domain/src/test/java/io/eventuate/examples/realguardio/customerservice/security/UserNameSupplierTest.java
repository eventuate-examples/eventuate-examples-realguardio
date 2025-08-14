package io.eventuate.examples.realguardio.customerservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes=UserNameSupplierTest.Config.class)
class UserNameSupplierTest {

    @Configuration
    @Import({SecurityConfiguration.class})
    @EnableAutoConfiguration
    public static class Config {
    }

    @Autowired
    private UserNameSupplier userNameSupplier;

    @Test
    @WithMockUser(username = "user@realguard.io")
    void shouldGetCurrentUserEmail() {
        String email = userNameSupplier.getCurrentUserEmail();
        assertThat(email).isEqualTo("user@realguard.io");
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        assertThatThrownBy(() -> userNameSupplier.getCurrentUserEmail())
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

}