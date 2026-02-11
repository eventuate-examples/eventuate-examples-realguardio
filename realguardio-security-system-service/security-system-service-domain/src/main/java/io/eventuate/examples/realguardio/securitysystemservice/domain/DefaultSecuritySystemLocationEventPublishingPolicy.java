package io.eventuate.examples.realguardio.securitysystemservice.domain;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default policy that publishes SecuritySystemAssignedToLocation events.
 * Active when the OsoLocalSecuritySystemLocation profile is NOT set.
 */
@Component
@Profile("!OsoLocalSecuritySystemLocation")
public class DefaultSecuritySystemLocationEventPublishingPolicy implements SecuritySystemLocationEventPublishingPolicy {

    @Override
    public boolean shouldPublishSecuritySystemAssignedToLocation() {
        return true;
    }
}
