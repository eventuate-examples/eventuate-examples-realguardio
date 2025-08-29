package io.realguardio.securitysystem.api;

import io.eventuate.tram.commands.common.Command;

public record UpdateCreationFailedCommand(Long securitySystemId, String rejectionReason) implements Command {
}