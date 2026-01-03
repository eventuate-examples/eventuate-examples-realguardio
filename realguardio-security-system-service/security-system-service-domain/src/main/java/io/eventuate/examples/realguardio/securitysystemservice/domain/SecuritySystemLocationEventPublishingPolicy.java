package io.eventuate.examples.realguardio.securitysystemservice.domain;

/**
 * Policy that determines whether SecuritySystemAssignedToLocation events should be published.
 * When using local authorization data bindings, the event is not needed since the
 * SecuritySystem-Location relationship is resolved locally via database queries.
 */
public interface SecuritySystemLocationEventPublishingPolicy {
    boolean shouldPublishSecuritySystemAssignedToLocation();
}
