package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationNoted;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemRepository;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemState;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;
import static io.eventuate.tram.sagas.participant.SagaReplyMessageBuilder.withLock;

public class SecuritySystemCommandHandler {

    private final SecuritySystemRepository repository;

    public SecuritySystemCommandHandler(SecuritySystemRepository repository) {
        this.repository = repository;
    }

    public Message handleCreateSecuritySystem(CommandMessage<CreateSecuritySystemCommand> cm) {
        CreateSecuritySystemCommand command = cm.getCommand();
        SecuritySystem securitySystem = new SecuritySystem(command.locationName(), SecuritySystemState.CREATION_PENDING);
        
        SecuritySystem savedSystem = repository.save(securitySystem);
        
        return withSuccess(new SecuritySystemCreated(savedSystem.getId()));
    }
    
    // Keep the simple version for tests
    public SecuritySystemCreated handle(CreateSecuritySystemCommand command) {
        SecuritySystem securitySystem = new SecuritySystem(command.locationName(), SecuritySystemState.CREATION_PENDING);
        
        SecuritySystem savedSystem = repository.save(securitySystem);
        
        return new SecuritySystemCreated(savedSystem.getId());
    }

    public Message handleNoteLocationCreated(CommandMessage<NoteLocationCreatedCommand> cm) {
        NoteLocationCreatedCommand command = cm.getCommand();
        SecuritySystem securitySystem = repository.findById(command.securitySystemId())
            .orElseThrow(() -> new IllegalArgumentException("Security system not found: " + command.securitySystemId()));
        
        securitySystem.setLocationId(command.locationId());
        securitySystem.setState(SecuritySystemState.DISARMED);
        
        repository.save(securitySystem);
        
        return withSuccess(new LocationNoted());
    }
    
    // Keep the simple version for tests
    public LocationNoted handle(NoteLocationCreatedCommand command) {
        SecuritySystem securitySystem = repository.findById(command.securitySystemId())
            .orElseThrow(() -> new IllegalArgumentException("Security system not found: " + command.securitySystemId()));
        
        securitySystem.setLocationId(command.locationId());
        securitySystem.setState(SecuritySystemState.DISARMED);
        
        repository.save(securitySystem);
        
        return new LocationNoted();
    }

    public CommandHandlers commandHandlers() {
        return SagaCommandHandlersBuilder
                .fromChannel("security-system-service")
                .onMessage(CreateSecuritySystemCommand.class, this::handleCreateSecuritySystem)
                .onMessage(NoteLocationCreatedCommand.class, this::handleNoteLocationCreated)
                .build();
    }
}