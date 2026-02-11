package io.realguardio.orchestration;

import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest
class OrchestrationServiceApplicationTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(TramInMemoryConfiguration.class)
    static class TestConfiguration {
    }

    @Test
    void contextLoads() {
    }
}