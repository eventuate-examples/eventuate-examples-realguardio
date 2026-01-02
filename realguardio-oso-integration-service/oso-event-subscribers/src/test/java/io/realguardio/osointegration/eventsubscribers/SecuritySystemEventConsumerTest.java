package io.realguardio.osointegration.eventsubscribers;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAssignedToLocation;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class SecuritySystemEventConsumerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import({OsoEventSubscribersConfiguration.class,
             TramInMemoryConfiguration.class})
    static class Config {
    }

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @MockBean
    private RealGuardOsoFactManager osoFactManager;

    @Test
    public void shouldHandleSecuritySystemAssignedToLocationFromSecuritySystemService() {
        Long securitySystemId = 101L;
        Long locationId = 789L;

        SecuritySystemAssignedToLocation event =
            new SecuritySystemAssignedToLocation(securitySystemId, locationId);

        domainEventPublisher.publish(
            "io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem",
            securitySystemId.toString(),
            Collections.singletonList(event));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(osoFactManager).assignSecuritySystemToLocation(
                securitySystemId.toString(),
                locationId.toString()
            );
        });
    }
}
