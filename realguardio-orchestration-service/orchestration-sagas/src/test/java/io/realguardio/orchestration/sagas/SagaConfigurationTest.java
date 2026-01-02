package io.realguardio.orchestration.sagas;

import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest
class SagaConfigurationTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({SagaConfiguration.class, SagaProxyConfiguration.class, TramInMemoryConfiguration.class})
    static class TestConfiguration {
    }

    @Autowired
    private CreateSecuritySystemWithLocationIdSaga saga;

    @Test
    void contextLoads() {
        // Bean injection verified by successful context initialization
    }
}