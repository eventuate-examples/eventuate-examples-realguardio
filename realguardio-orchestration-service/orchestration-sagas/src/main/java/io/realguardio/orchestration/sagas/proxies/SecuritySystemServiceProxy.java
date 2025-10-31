package io.realguardio.orchestration.sagas.proxies;

import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.commands.consumer.CommandWithDestinationBuilder;
import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantOperation;
import io.eventuate.tram.sagas.simpledsl.annotations.SagaParticipantProxy;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.*;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.*;

@SagaParticipantProxy(channel = SecuritySystemServiceProxy.CHANNEL)
public class SecuritySystemServiceProxy {

    public static final String CHANNEL = "security-system-service";

    @SagaParticipantOperation(
        commandClass = CreateSecuritySystemCommand.class,
        replyClasses = SecuritySystemCreated.class
    )
    public CommandWithDestination createSecuritySystem(String locationName) {
        return CommandWithDestinationBuilder.send(new CreateSecuritySystemCommand(locationName))
                .to(CHANNEL)
                .build();
    }

    @SagaParticipantOperation(
        commandClass = UpdateCreationFailedCommand.class,
        replyClasses = Void.class
    )
    public CommandWithDestination updateCreationFailed(Long securitySystemId, String rejectionReason) {
        return CommandWithDestinationBuilder.send(new UpdateCreationFailedCommand(securitySystemId, rejectionReason))
                .to(CHANNEL)
                .build();
    }

    @SagaParticipantOperation(
        commandClass = NoteLocationCreatedCommand.class,
        replyClasses = LocationNoted.class
    )
    public CommandWithDestination noteLocationCreated(Long securitySystemId, Long locationId) {
        return CommandWithDestinationBuilder.send(new NoteLocationCreatedCommand(securitySystemId, locationId))
                .to(CHANNEL)
                .build();
    }
}