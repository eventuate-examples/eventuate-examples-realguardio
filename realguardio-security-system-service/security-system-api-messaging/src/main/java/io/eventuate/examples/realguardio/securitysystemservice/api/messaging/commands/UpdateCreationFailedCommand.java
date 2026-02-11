package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.commands;

import io.eventuate.tram.commands.common.Command;

public record UpdateCreationFailedCommand(Long securitySystemId, String rejectionReason) implements Command {
}