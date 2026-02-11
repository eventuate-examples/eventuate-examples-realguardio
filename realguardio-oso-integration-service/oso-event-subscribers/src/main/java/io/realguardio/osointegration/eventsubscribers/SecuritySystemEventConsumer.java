package io.realguardio.osointegration.eventsubscribers;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemAssignedToLocation;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.annotations.EventuateDomainEventHandler;
import io.realguardio.osointegration.ososervice.RealGuardOsoFactManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecuritySystemEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SecuritySystemEventConsumer.class);

    private final RealGuardOsoFactManager osoFactManager;

    @Autowired
    public SecuritySystemEventConsumer(RealGuardOsoFactManager osoFactManager) {
        this.osoFactManager = osoFactManager;
    }

    @EventuateDomainEventHandler(subscriberId = "osoEventSubscribersDispatcher", channel = "io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem")
    public void handleSecuritySystemAssignedToLocation(DomainEventEnvelope<SecuritySystemAssignedToLocation> envelope) {
        SecuritySystemAssignedToLocation event = envelope.getEvent();

        logger.info("Handling SecuritySystemAssignedToLocation event from Security System Service: securitySystemId={}, locationId={}",
                   event.securitySystemId(), event.locationId());

        osoFactManager.assignSecuritySystemToLocation(
            event.securitySystemId().toString(),
            event.locationId().toString()
        );
    }
}
