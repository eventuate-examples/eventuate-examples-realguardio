package io.realguardio.securitysystem.api;

import io.eventuate.tram.commands.common.Command;

public record NoteLocationCreatedCommand(Long securitySystemId, Long locationId) implements Command {
}