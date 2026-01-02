package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies;

import io.eventuate.tram.commands.consumer.annotations.FailureReply;

@FailureReply
public record LocationAlreadyHasSecuritySystem(Long locationId) {
}
