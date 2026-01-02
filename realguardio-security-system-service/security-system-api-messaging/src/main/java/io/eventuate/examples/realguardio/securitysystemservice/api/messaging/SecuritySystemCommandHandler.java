package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemWithLocationIdCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.NoteLocationCreatedCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationNoted;
import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemService;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.commands.consumer.annotations.EventuateCommandHandler;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class SecuritySystemCommandHandler {

    private static Logger logger = getLogger(SecuritySystemCommandHandler.class);

    private final SecuritySystemService securitySystemService;

    public SecuritySystemCommandHandler(SecuritySystemService securitySystemService) {
        this.securitySystemService = securitySystemService;
    }

    @EventuateCommandHandler(subscriberId = "securitySystemCommandDispatcher", channel = "security-system-service")
    public SecuritySystemCreated handleCreateSecuritySystem(CommandMessage<CreateSecuritySystemCommand> cm) {
        logger.info("Handling CreateSecuritySystemCommand: " + cm);
        CreateSecuritySystemCommand command = cm.getCommand();
        Long securitySystemId = securitySystemService.createSecuritySystem(command.locationName());
        logger.info("Created CreateSecuritySystemCommand: " + cm);
        return new SecuritySystemCreated(securitySystemId);
    }

    @EventuateCommandHandler(subscriberId = "securitySystemCommandDispatcher", channel = "security-system-service")
    public LocationNoted handleNoteLocationCreated(CommandMessage<NoteLocationCreatedCommand> cm) {
        NoteLocationCreatedCommand command = cm.getCommand();
        securitySystemService.noteLocationCreated(command.securitySystemId(), command.locationId());
        return new LocationNoted();
    }

    @EventuateCommandHandler(subscriberId = "securitySystemCommandDispatcher", channel = "security-system-service")
    public SecuritySystemCreated handleCreateSecuritySystemWithLocationId(CommandMessage<CreateSecuritySystemWithLocationIdCommand> cm) {
        logger.info("Handling CreateSecuritySystemWithLocationIdCommand: " + cm);
        CreateSecuritySystemWithLocationIdCommand command = cm.getCommand();
        Long securitySystemId = securitySystemService.createSecuritySystemWithLocation(command.locationId(), command.locationName());
        logger.info("Created SecuritySystem with locationId: " + command.locationId());
        return new SecuritySystemCreated(securitySystemId);
    }
}