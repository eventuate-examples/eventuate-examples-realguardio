package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands;

import io.eventuate.tram.commands.common.Command;

public record CreateSecuritySystemCommand(String locationName) implements Command {
}