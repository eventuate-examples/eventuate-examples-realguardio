package io.eventuate.examples.realguardio.customerservice.api.messaging.commands;

import io.eventuate.tram.commands.common.Command;

public record CreateLocationWithSecuritySystemCommand(
    Long customerId, 
    String locationName, 
    Long securitySystemId
) implements Command {
}