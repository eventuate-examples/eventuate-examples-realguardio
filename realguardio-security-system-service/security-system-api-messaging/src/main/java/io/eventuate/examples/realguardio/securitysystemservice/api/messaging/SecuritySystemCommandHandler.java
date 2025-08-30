package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationNoted;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class SecuritySystemCommandHandler {

    private final SecuritySystemService securitySystemService;

    public SecuritySystemCommandHandler(SecuritySystemService securitySystemService) {
        this.securitySystemService = securitySystemService;
    }

    public Message handleCreateSecuritySystem(CommandMessage<CreateSecuritySystemCommand> cm) {
        CreateSecuritySystemCommand command = cm.getCommand();
        Long securitySystemId = securitySystemService.createSecuritySystem(command.locationName());
        return withSuccess(new SecuritySystemCreated(securitySystemId));
    }

    public Message handleNoteLocationCreated(CommandMessage<NoteLocationCreatedCommand> cm) {
        NoteLocationCreatedCommand command = cm.getCommand();
        securitySystemService.noteLocationCreated(command.securitySystemId(), command.locationId());
        return withSuccess(new LocationNoted());
    }

    public CommandHandlers commandHandlers() {
        return SagaCommandHandlersBuilder
                .fromChannel("security-system-service")
                .onMessage(CreateSecuritySystemCommand.class, this::handleCreateSecuritySystem)
                .onMessage(NoteLocationCreatedCommand.class, this::handleNoteLocationCreated)
                .build();
    }
}