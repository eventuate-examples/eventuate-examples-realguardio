package io.eventuate.examples.realguardio.securitysystemservice.api.messaging;

import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands.CreateSecuritySystemCommand;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.LocationAlreadyHasSecuritySystem;
import io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies.SecuritySystemCreated;
import io.eventuate.examples.realguardio.securitysystemservice.domain.LocationAlreadyHasSecuritySystemException;
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
    public Object handleCreateSecuritySystem(CommandMessage<CreateSecuritySystemCommand> cm) {
        logger.info("Handling CreateSecuritySystemCommand: " + cm);
        CreateSecuritySystemCommand command = cm.getCommand();
        try {
            Long securitySystemId = securitySystemService.createSecuritySystemWithLocation(command.locationId(), command.locationName());
            logger.info("Created SecuritySystem with locationId: " + command.locationId());
            return new SecuritySystemCreated(securitySystemId);
        } catch (LocationAlreadyHasSecuritySystemException e) {
            logger.info("Location {} already has a SecuritySystem", command.locationId());
            return new LocationAlreadyHasSecuritySystem(command.locationId());
        }
    }
}