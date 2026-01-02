package io.eventuate.examples.realguardio.customerservice.api.messaging.commands;

import io.eventuate.tram.commands.common.Command;

public record ValidateLocationCommand(Long locationId) implements Command {
}
