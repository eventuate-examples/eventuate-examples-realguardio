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

import java.util.Collections;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class SecuritySystemCommandHandler {

    private final SecuritySystemRepository repository;

    public SecuritySystemCommandHandler(SecuritySystemRepository repository) {
        this.repository = repository;
    }

    public SecuritySystemCreated handle(CreateSecuritySystemCommand command) {
        SecuritySystem securitySystem = new SecuritySystem(command.locationName(), SecuritySystemState.CREATION_PENDING);
        
        SecuritySystem savedSystem = repository.save(securitySystem);
        
        return new SecuritySystemCreated(savedSystem.getId());
    }

    public LocationNoted handle(NoteLocationCreatedCommand command) {
        SecuritySystem securitySystem = repository.findById(command.securitySystemId())
            .orElseThrow(() -> new IllegalArgumentException("Security system not found: " + command.securitySystemId()));
        
        securitySystem.setLocationId(command.locationId());
        securitySystem.setState(SecuritySystemState.DISARMED);
        
        repository.save(securitySystem);
        
        return new LocationNoted();
    }
}