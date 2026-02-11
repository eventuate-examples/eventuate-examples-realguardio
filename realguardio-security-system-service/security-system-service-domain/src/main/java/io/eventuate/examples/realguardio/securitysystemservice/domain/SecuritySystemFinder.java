package io.eventuate.examples.realguardio.securitysystemservice.domain;

import java.util.List;

public interface SecuritySystemFinder {
    List<SecuritySystemProjection> findAllAccessible(String userName);
}
