package io.eventuate.examples.realguardio.securitysystemservice.osointegration;

import io.eventuate.examples.realguardio.securitysystemservice.domain.SecuritySystemLocationEventPublishingPolicy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Policy that suppresses SecuritySystemAssignedToLocation events.
 * Active when the OsoLocalSecuritySystemLocation profile IS set.
 *
 * When using local authorization data bindings, the SecuritySystem-Location
 * relationship is resolved via local database queries, so the event is not needed.
 */
@Component
@Profile("OsoLocalSecuritySystemLocation")
public class LocalSecuritySystemLocationEventPublishingPolicy implements SecuritySystemLocationEventPublishingPolicy {

    @Override
    public boolean shouldPublishSecuritySystemAssignedToLocation() {
        return false;
    }
}
