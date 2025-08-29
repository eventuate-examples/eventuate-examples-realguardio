package io.realguardio.customer.api;

import io.eventuate.tram.commands.common.Command;

public record CreateLocationWithSecuritySystemCommand(
    Long customerId, 
    String locationName, 
    Long securitySystemId
) implements Command {
}