package io.eventuate.examples.realguardio.customerservice.customermanagement.domain;

import io.eventuate.examples.realguardio.customerservice.domain.CustomerEvent;

public record TeamMemberAdded(
    Long teamId,
    Long customerEmployeeId
) implements CustomerEvent {
}
