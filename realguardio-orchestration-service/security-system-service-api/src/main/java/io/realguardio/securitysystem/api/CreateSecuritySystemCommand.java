package io.realguardio.securitysystem.api;

import io.eventuate.tram.commands.common.Command;

public record CreateSecuritySystemCommand(String locationName) implements Command {
}