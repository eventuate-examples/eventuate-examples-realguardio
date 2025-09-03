package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.commands.consumer.CommandWithDestinationBuilder;
import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantProxy;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.*;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.*;

@SagaParticipantProxy(channel = SecuritySystemServiceProxy.CHANNEL)
public class SecuritySystemServiceProxy {

    public static final String CHANNEL = "security-system-service";

    public CommandWithDestination createSecuritySystem(String locationName) {
        return CommandWithDestinationBuilder.send(new CreateSecuritySystemCommand(locationName))
                .to(CHANNEL)
                .build();
    }

    public CommandWithDestination updateCreationFailed(Long securitySystemId, String rejectionReason) {
        return CommandWithDestinationBuilder.send(new UpdateCreationFailedCommand(securitySystemId, rejectionReason))
                .to(CHANNEL)
                .build();
    }

    public CommandWithDestination noteLocationCreated(Long securitySystemId, Long locationId) {
        return CommandWithDestinationBuilder.send(new NoteLocationCreatedCommand(securitySystemId, locationId))
                .to(CHANNEL)
                .build();
    }
}