package io.eventuate.examples.realguardio.customerservice.api.messaging.replies;

import io.eventuate.tram.commands.consumer.annotations.FailureReply;

@FailureReply
public record LocationNotFound() {
}
