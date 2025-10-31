package io.eventuate.examples.realguardio.customerservice.api.messaging.replies;

import io.eventuate.tram.commands.consumer.annotations.SuccessReply;

@SuccessReply
public record LocationCreatedWithSecuritySystem(Long locationId) implements CreateLocationResult {
}