package io.realguardio.customer.api;

import io.eventuate.tram.commands.consumer.annotations.FailureReply;

@FailureReply
public record LocationAlreadyHasSecuritySystem() {
}