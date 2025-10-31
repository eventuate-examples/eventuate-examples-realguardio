package io.eventuate.examples.realguardio.securitysystemservice.api.messaging.replies;

import io.eventuate.tram.commands.consumer.annotations.SuccessReply;

@SuccessReply
public record LocationNoted() {
}