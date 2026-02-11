package io.eventuate.examples.realguardio.customerservice.api.messaging.replies;

import io.eventuate.tram.commands.consumer.annotations.SuccessReply;

@SuccessReply
public record LocationValidated(Long locationId, String locationName, Long customerId) implements ValidateLocationResult {
}
