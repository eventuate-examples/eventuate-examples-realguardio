package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClasspathLocalAuthorizationConfigFileSupplierTest {

    private ClasspathLocalAuthorizationConfigFileSupplier provider;

    @BeforeEach
    public void beforeEach() {
        provider = new ClasspathLocalAuthorizationConfigFileSupplier();
    }

    @Test
    void should() {
        var path = provider.get();
        assertThat(path).isNotNull();
    }

}